package com.example.demo.service;

import com.example.demo.dto.request.CreateUserRequest;
import com.example.demo.entity.User;
import com.example.demo.entity.Wallet;
import com.example.demo.exception.PaymentException;
import com.example.demo.repository.user.UserRepository;
import com.example.demo.repository.user.WalletRepository;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * User Service - Manages user creation and operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;

    /**
     * Create a new user and automatically create a wallet for them
     */
    @Transactional
    @Observed
    public User createUser(CreateUserRequest request) {
        // Check if username already exists
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new PaymentException("Username already exists: " + request.getUsername());
        }

        // Check if email already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new PaymentException("Email already exists: " + request.getEmail());
        }

        // Create user
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .isVerified(request.getIsVerified() != null ? request.getIsVerified() : false)
                .build();

        User savedUser = userRepository.save(user);

        // Automatically create wallet for the user
        Wallet wallet = Wallet.builder()
                .userId(savedUser.getId())
                .balance(BigDecimal.ZERO)
                .currency("USD")
                .isActive(true)
                .build();

        walletRepository.save(wallet);

        log.info("User created with wallet. UserId: {}, Username: {}", savedUser.getId(), savedUser.getUsername());
        return savedUser;
    }
}
