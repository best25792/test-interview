package com.example.userservice.controller;

import com.example.userservice.client.WalletClientService;
import com.example.userservice.dto.request.CreateUserRequest;
import com.example.userservice.entity.User;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final WalletClientService walletClientService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<User> createUser(@Valid @RequestBody CreateUserRequest request) {
        User user = userService.createUser(request);
        return new ResponseEntity<>(user, HttpStatus.CREATED);
    }

    @GetMapping("/{id}/wallet/balance")
    public ResponseEntity<WalletBalanceResponse> getWalletBalance(@PathVariable Long id) {
        BigDecimal balance = walletClientService.getWalletBalance(id);
        return ResponseEntity.ok(new WalletBalanceResponse(id, balance));
    }

    @GetMapping("/{id}/wallet")
    public ResponseEntity<WalletResponse> getWallet(@PathVariable Long id) {
        WalletClientService.WalletResponse wallet = walletClientService.getWallet(id);
        return ResponseEntity.ok(new WalletResponse(
                wallet.id(),
                wallet.userId(),
                wallet.balance(),
                wallet.currency(),
                wallet.isActive(),
                wallet.createdAt(),
                wallet.updatedAt()
        ));
    }

    @PostMapping("/{id}/wallet/topup")
    public ResponseEntity<WalletBalanceResponse> topUpWallet(
            @PathVariable Long id,
            @RequestBody TopUpRequest request) {
        BigDecimal balance = walletClientService.topUpWallet(id, request.amount());
        return ResponseEntity.ok(new WalletBalanceResponse(id, balance));
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        return ResponseEntity.ok(user);
    }

    // DTOs
    record WalletBalanceResponse(Long userId, BigDecimal balance) {}
    record WalletResponse(Long id, Long userId, BigDecimal balance, String currency, Boolean isActive, 
                         java.time.LocalDateTime createdAt, java.time.LocalDateTime updatedAt) {}
    record TopUpRequest(BigDecimal amount) {}
}
