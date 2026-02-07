package com.example.demo.service;

import com.example.demo.entity.Hold;
import com.example.demo.entity.HoldStatus;
import com.example.demo.entity.User;
import com.example.demo.entity.Wallet;
import com.example.demo.event.EventPublisher;
import com.example.demo.event.HoldCapturedEvent;
import com.example.demo.event.HoldCreatedEvent;
import com.example.demo.event.HoldReleasedEvent;
import com.example.demo.event.WalletDeductedEvent;
import com.example.demo.exception.PaymentException;
import com.example.demo.repository.wallet.HoldRepository;
import com.example.demo.repository.user.UserRepository;
import com.example.demo.repository.user.WalletRepository;
import com.example.demo.config.TracingConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Wallet Service - Simulated as a separate microservice
 * Manages user wallet balances, holds, and authorizations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final HoldRepository holdRepository;
    private final EventPublisher eventPublisher;
    private final TracingConfig tracingConfig;

    /**
     * Get or create wallet for user
     */
    @Transactional
    public Wallet getOrCreateWallet(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseGet(() -> {
                    // Verify user exists
                    userRepository.findById(userId)
                            .orElseThrow(() -> new PaymentException("User not found with id: " + userId));
                    
                    // Create wallet
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
     * Get available balance (wallet balance minus active holds)
     */
    public BigDecimal getAvailableBalance(Long userId) {
        Wallet wallet = getOrCreateWallet(userId);
        
        BigDecimal totalHeldAmount = holdRepository.getTotalHeldAmountByUserAndStatus(
                userId, HoldStatus.ACTIVE);
        
        if (totalHeldAmount == null) {
            totalHeldAmount = BigDecimal.ZERO;
        }
        
        return wallet.getBalance().subtract(totalHeldAmount);
    }

    /**
     * Check if user has sufficient available balance (considering holds)
     */
    public boolean hasSufficientBalance(Long userId, BigDecimal amount) {
        BigDecimal availableBalance = getAvailableBalance(userId);
        return availableBalance.compareTo(amount) >= 0;
    }

    /**
     * Check user business conditions
     */
    public boolean validateUserConditions(Long userId) {
        User user = userRepository.findByIdWithLock(userId)
                .orElseThrow(() -> new PaymentException("User not found with id: " + userId));
        
        // Business conditions:
        // 1. User must be active
        if (!user.getIsActive()) {
            log.warn("User {} is not active", userId);
            return false;
        }
        
        // 2. Wallet must be active
        Wallet wallet = getOrCreateWallet(userId);
        if (!wallet.getIsActive()) {
            log.warn("Wallet for user {} is not active", userId);
            return false;
        }
        
        return true;
    }

    /**
     * Create a hold (authorization) on user's wallet
     * This reserves the amount but doesn't deduct it yet
     */
    @Transactional
    public Hold createHold(Long userId, Long paymentId, BigDecimal amount, String reason) {
        // Add payment_id to trace context and MDC for correlation
        if (paymentId != null) {
            tracingConfig.addPaymentIdToContext(paymentId);
        }
        
        Wallet wallet = walletRepository.findByUserIdWithLock(userId)
                .orElseGet(() -> getOrCreateWallet(userId));
        
        // Check available balance (wallet balance minus active holds)
        BigDecimal availableBalance = getAvailableBalance(userId);
        if (availableBalance.compareTo(amount) < 0) {
            throw new PaymentException("Insufficient available balance. Available: " + 
                    availableBalance + ", Required: " + amount);
        }
        
        // Create hold record
        Hold hold = Hold.builder()
                .userId(userId)
                .paymentId(paymentId)
                .amount(amount)
                .status(HoldStatus.ACTIVE)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .reason(reason)
                .build();
        
        Hold savedHold = holdRepository.save(hold);
        
        // Publish hold created event
        HoldCreatedEvent event = HoldCreatedEvent.builder()
                .holdId(savedHold.getId())
                .userId(userId)
                .paymentId(paymentId)
                .amount(amount)
                .expiresAt(savedHold.getExpiresAt())
                .timestamp(LocalDateTime.now())
                .build();
        
        eventPublisher.publishHoldCreated(event);
        
        log.info("Hold created. HoldId: {}, UserId: {}, PaymentId: {}, Amount: {}", 
                savedHold.getId(), userId, paymentId, amount);
        
        return savedHold;
    }

    /**
     * Capture a hold (deduct from wallet)
     * This is called when payment is confirmed
     */
    @Transactional
    public void captureHold(Long holdId, String reason) {
        Hold hold = holdRepository.findByIdWithLock(holdId)
                .orElseThrow(() -> new PaymentException("Hold not found with id: " + holdId));
        
        // Add payment_id to trace context and MDC for correlation
        if (hold.getPaymentId() != null) {
            tracingConfig.addPaymentIdToContext(hold.getPaymentId());
        }
        
        if (hold.getStatus() != HoldStatus.ACTIVE) {
            throw new PaymentException("Hold is not active. Current status: " + hold.getStatus());
        }
        
        Wallet wallet = walletRepository.findByUserIdWithLock(hold.getUserId())
                .orElseThrow(() -> new PaymentException("Wallet not found for user id: " + hold.getUserId()));
        
        // Verify wallet still has sufficient balance (safety check)
        if (wallet.getBalance().compareTo(hold.getAmount()) < 0) {
            throw new PaymentException("Insufficient wallet balance to capture hold");
        }
        
        // Deduct from wallet
        BigDecimal oldBalance = wallet.getBalance();
        wallet.setBalance(oldBalance.subtract(hold.getAmount()));
        Wallet updatedWallet = walletRepository.save(wallet);
        
        // Update hold status to CAPTURED
        hold.setStatus(HoldStatus.CAPTURED);
        hold.setCapturedAt(LocalDateTime.now());
        holdRepository.save(hold);
        
        // Publish events
        HoldCapturedEvent holdEvent = HoldCapturedEvent.builder()
                .holdId(holdId)
                .userId(hold.getUserId())
                .paymentId(hold.getPaymentId())
                .amount(hold.getAmount())
                .oldBalance(oldBalance)
                .newBalance(updatedWallet.getBalance())
                .timestamp(LocalDateTime.now())
                .build();
        
        eventPublisher.publishHoldCaptured(holdEvent);
        
        WalletDeductedEvent walletEvent = WalletDeductedEvent.builder()
                .userId(hold.getUserId())
                .paymentId(hold.getPaymentId())
                .amount(hold.getAmount())
                .oldBalance(oldBalance)
                .newBalance(updatedWallet.getBalance())
                .reason(reason != null ? reason : "Hold captured")
                .timestamp(LocalDateTime.now())
                .build();
        
        eventPublisher.publishWalletDeducted(walletEvent);
        
        log.info("Hold captured. HoldId: {}, UserId: {}, PaymentId: {}, Amount: {}, " +
                "Old Balance: {}, New Balance: {}", 
                holdId, hold.getUserId(), hold.getPaymentId(), hold.getAmount(),
                oldBalance, updatedWallet.getBalance());
    }

    /**
     * Release a hold (cancel authorization)
     * This is called when payment fails, is cancelled, or expires
     */
    @Transactional
    public void releaseHold(Long holdId, String reason) {
        Hold hold = holdRepository.findByIdWithLock(holdId)
                .orElseThrow(() -> new PaymentException("Hold not found with id: " + holdId));
        
        // Add payment_id to trace context and MDC for correlation
        if (hold.getPaymentId() != null) {
            tracingConfig.addPaymentIdToContext(hold.getPaymentId());
        }
        
        if (hold.getStatus() != HoldStatus.ACTIVE) {
            log.warn("Attempting to release non-active hold. HoldId: {}, Status: {}", 
                    holdId, hold.getStatus());
            return;
        }
        
        // Update hold status to RELEASED
        hold.setStatus(HoldStatus.RELEASED);
        hold.setReleasedAt(LocalDateTime.now());
        hold.setReason(reason);
        holdRepository.save(hold);
        
        // Publish hold released event
        HoldReleasedEvent event = HoldReleasedEvent.builder()
                .holdId(holdId)
                .userId(hold.getUserId())
                .paymentId(hold.getPaymentId())
                .amount(hold.getAmount())
                .reason(reason)
                .timestamp(LocalDateTime.now())
                .build();
        
        eventPublisher.publishHoldReleased(event);
        
        log.info("Hold released. HoldId: {}, UserId: {}, PaymentId: {}, Amount: {}, Reason: {}", 
                holdId, hold.getUserId(), hold.getPaymentId(), hold.getAmount(), reason);
    }

    /**
     * Get hold by payment ID
     */
    public Hold getHoldByPaymentId(Long paymentId) {
        return holdRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new PaymentException("Hold not found for paymentId: " + paymentId));
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
     * Get wallet entity
     */
    public Wallet getWallet(Long userId) {
        return getOrCreateWallet(userId);
    }

    /**
     * Legacy method - direct deduction (without hold)
     * Kept for backward compatibility, but prefer using holds
     */
    @Transactional
    @Deprecated
    public void deductFromWallet(Long userId, BigDecimal amount, Long paymentId, String reason) {
        Wallet wallet = walletRepository.findByUserIdWithLock(userId)
                .orElseThrow(() -> new PaymentException("Wallet not found for user id: " + userId));
        
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new PaymentException("Insufficient wallet balance. Current: " + 
                    wallet.getBalance() + ", Required: " + amount);
        }
        
        BigDecimal oldBalance = wallet.getBalance();
        wallet.setBalance(oldBalance.subtract(amount));
        Wallet updatedWallet = walletRepository.save(wallet);
        
        WalletDeductedEvent event = WalletDeductedEvent.builder()
                .userId(userId)
                .paymentId(paymentId)
                .amount(amount)
                .oldBalance(oldBalance)
                .newBalance(updatedWallet.getBalance())
                .reason(reason)
                .timestamp(LocalDateTime.now())
                .build();
        
        eventPublisher.publishWalletDeducted(event);
        
        log.info("Wallet deducted. UserId: {}, Amount: {}, Old Balance: {}, New Balance: {}", 
                userId, amount, oldBalance, updatedWallet.getBalance());
    }
}
