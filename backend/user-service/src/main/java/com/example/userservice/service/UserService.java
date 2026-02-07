package com.example.userservice.service;

import com.example.userservice.dto.request.CreateUserRequest;
import com.example.userservice.entity.User;
import com.example.userservice.exception.PaymentException;
import com.example.userservice.repository.UserRepository;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User Service - Manages user creation and operations
 * Note: Wallet operations are handled by wallet-service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    /**
     * Create a new user and automatically create a wallet for them
     */
    @Transactional
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

        // Note: Wallet creation is now handled by wallet-service
        // When wallet operations are needed, they should call wallet-service API

        log.info("User created. UserId: {}, Username: {}", savedUser.getId(), savedUser.getUsername());
        return savedUser;
    }
}
