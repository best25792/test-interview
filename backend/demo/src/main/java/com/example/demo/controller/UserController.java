package com.example.demo.controller;

import com.example.demo.dto.request.CreateUserRequest;
import com.example.demo.entity.User;
import com.example.demo.entity.Wallet;
import com.example.demo.repository.user.UserRepository;
import com.example.demo.service.UserService;
import com.example.demo.service.WalletService;
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
    private final WalletService walletService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<User> createUser(@Valid @RequestBody CreateUserRequest request) {
        User user = userService.createUser(request);
        return new ResponseEntity<>(user, HttpStatus.CREATED);
    }

    @GetMapping("/{id}/wallet/balance")
    public ResponseEntity<WalletBalanceResponse> getWalletBalance(@PathVariable Long id) {
        BigDecimal balance = walletService.getWalletBalance(id);
        return ResponseEntity.ok(new WalletBalanceResponse(id, balance));
    }

    @GetMapping("/{id}/wallet/available-balance")
    public ResponseEntity<AvailableBalanceResponse> getAvailableBalance(@PathVariable Long id) {
        BigDecimal availableBalance = walletService.getAvailableBalance(id);
        BigDecimal totalBalance = walletService.getWalletBalance(id);
        return ResponseEntity.ok(new AvailableBalanceResponse(id, totalBalance, availableBalance));
    }

    @GetMapping("/{id}/wallet")
    public ResponseEntity<WalletResponse> getWallet(@PathVariable Long id) {
        Wallet wallet = walletService.getWallet(id);
        return ResponseEntity.ok(new WalletResponse(
                wallet.getId(),
                wallet.getUserId(),
                wallet.getBalance(),
                wallet.getCurrency(),
                wallet.getIsActive(),
                wallet.getCreatedAt(),
                wallet.getUpdatedAt()
        ));
    }

    @PostMapping("/{id}/wallet/topup")
    public ResponseEntity<WalletBalanceResponse> topUpWallet(
            @PathVariable Long id,
            @RequestBody TopUpRequest request) {
        walletService.addToWallet(id, request.amount(), "Top-up");
        BigDecimal balance = walletService.getWalletBalance(id);
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
    record AvailableBalanceResponse(Long userId, BigDecimal totalBalance, BigDecimal availableBalance) {}
    record WalletResponse(Long id, Long userId, BigDecimal balance, String currency, Boolean isActive, 
                         java.time.LocalDateTime createdAt, java.time.LocalDateTime updatedAt) {}
    record TopUpRequest(BigDecimal amount) {}
}
