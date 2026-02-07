package com.example.walletservice.service;

import com.example.walletservice.entity.Wallet;
import com.example.walletservice.exception.PaymentException;
import com.example.walletservice.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Wallet Service - Manages wallet balance operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    private final WalletRepository walletRepository;

    /**
     * Get or create wallet for user
     */
    @Transactional
    public Wallet getOrCreateWallet(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseGet(() -> {
                    // Create wallet if it doesn't exist
                    Wallet wallet = Wallet.builder()
                            .userId(userId)
                            .balance(BigDecimal.ZERO)
                            .currency("USD")
                            .isActive(true)
                            .build();
                    
                    return walletRepository.save(wallet);
                });
    }

    /**
     * Get wallet balance (total balance, not considering holds)
     */
    public BigDecimal getWalletBalance(Long userId) {
        Wallet wallet = getOrCreateWallet(userId);
        return wallet.getBalance();
    }

    /**
     * Add amount to user wallet (for refunds, top-ups, etc.)
     */
    @Transactional
    public void addToWallet(Long userId, BigDecimal amount, String reason) {
        Wallet wallet = walletRepository.findByUserIdWithLock(userId)
                .orElseGet(() -> getOrCreateWallet(userId));
        
        BigDecimal oldBalance = wallet.getBalance();
        wallet.setBalance(oldBalance.add(amount));
        Wallet updatedWallet = walletRepository.save(wallet);
        
        log.info("Wallet credited. UserId: {}, Amount: {}, Old Balance: {}, New Balance: {}, Reason: {}", 
                userId, amount, oldBalance, updatedWallet.getBalance(), reason);
    }

    /**
     * Deduct amount from user wallet
     */
    @Transactional
    public void deductFromWallet(Long userId, BigDecimal amount) {
        Wallet wallet = walletRepository.findByUserIdWithLock(userId)
                .orElseThrow(() -> new PaymentException("Wallet not found for user id: " + userId));
        
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new PaymentException("Insufficient wallet balance. Current: " + 
                    wallet.getBalance() + ", Required: " + amount);
        }
        
        BigDecimal oldBalance = wallet.getBalance();
        wallet.setBalance(oldBalance.subtract(amount));
        Wallet updatedWallet = walletRepository.save(wallet);
        
        log.info("Wallet deducted. UserId: {}, Amount: {}, Old Balance: {}, New Balance: {}", 
                userId, amount, oldBalance, updatedWallet.getBalance());
    }

    /**
     * Get wallet entity
     */
    public Wallet getWallet(Long userId) {
        return getOrCreateWallet(userId);
    }
}
