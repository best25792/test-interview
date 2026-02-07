package com.example.walletservice.controller;

import com.example.walletservice.entity.Wallet;
import com.example.walletservice.service.WalletService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping("/users/{userId}/balance")
    public ResponseEntity<WalletBalanceResponse> getWalletBalance(@PathVariable Long userId) {
        BigDecimal balance = walletService.getWalletBalance(userId);
        return ResponseEntity.ok(new WalletBalanceResponse(userId, balance));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<WalletResponse> getWallet(@PathVariable Long userId) {
        Wallet wallet = walletService.getWallet(userId);
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

    @PostMapping("/users/{userId}/topup")
    public ResponseEntity<WalletBalanceResponse> topUpWallet(
            @PathVariable Long userId,
            @RequestBody TopUpRequest request) {
        walletService.addToWallet(userId, request.amount(), "Top-up");
        BigDecimal balance = walletService.getWalletBalance(userId);
        return ResponseEntity.ok(new WalletBalanceResponse(userId, balance));
    }

    @PostMapping("/users/{userId}/deduct")
    public ResponseEntity<Void> deductFromWallet(
            @PathVariable Long userId,
            @RequestBody DeductRequest request) {
        walletService.deductFromWallet(userId, request.amount());
        return ResponseEntity.ok().build();
    }

    // DTOs
    record WalletBalanceResponse(Long userId, BigDecimal balance) {}
    record WalletResponse(Long id, Long userId, BigDecimal balance, String currency, Boolean isActive, 
                         java.time.LocalDateTime createdAt, java.time.LocalDateTime updatedAt) {}
    record TopUpRequest(BigDecimal amount) {}
    record DeductRequest(BigDecimal amount) {}
}
