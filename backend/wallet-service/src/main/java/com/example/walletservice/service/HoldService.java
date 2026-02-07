package com.example.walletservice.service;

import com.example.walletservice.entity.Hold;
import com.example.walletservice.entity.HoldStatus;
import com.example.walletservice.exception.PaymentException;
import com.example.walletservice.repository.HoldRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Hold Service - Manages holds (authorizations) on user wallets
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HoldService {

    private final HoldRepository holdRepository;
    private final WalletService walletService;

    /**
     * Get available balance (wallet balance minus active holds)
     */
    public BigDecimal getAvailableBalance(Long userId) {
        BigDecimal walletBalance = walletService.getWalletBalance(userId);
        BigDecimal totalHeldAmount = holdRepository.getTotalHeldAmountByUserAndStatus(
                userId, HoldStatus.ACTIVE);
        
        if (totalHeldAmount == null) {
            totalHeldAmount = BigDecimal.ZERO;
        }
        
        return walletBalance.subtract(totalHeldAmount);
    }

    /**
     * Create a hold (authorization) on user's wallet
     */
    @Transactional
    public Hold createHold(Long userId, Long paymentId, BigDecimal amount, String reason) {
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
        
        log.info("Hold created. HoldId: {}, UserId: {}, PaymentId: {}, Amount: {}", 
                savedHold.getId(), userId, paymentId, amount);
        
        return savedHold;
    }

    /**
     * Capture a hold (deduct from wallet)
     */
    @Transactional
    public void captureHold(Long holdId, String reason) {
        Hold hold = holdRepository.findByIdWithLock(holdId)
                .orElseThrow(() -> new PaymentException("Hold not found with id: " + holdId));
        
        if (hold.getStatus() != HoldStatus.ACTIVE) {
            throw new PaymentException("Hold is not active. Current status: " + hold.getStatus());
        }
        
        // Deduct from wallet
        walletService.deductFromWallet(hold.getUserId(), hold.getAmount());
        
        // Update hold status to CAPTURED
        hold.setStatus(HoldStatus.CAPTURED);
        hold.setCapturedAt(LocalDateTime.now());
        holdRepository.save(hold);
        
        log.info("Hold captured. HoldId: {}, UserId: {}, PaymentId: {}, Amount: {}", 
                holdId, hold.getUserId(), hold.getPaymentId(), hold.getAmount());
    }

    /**
     * Release a hold (cancel authorization)
     */
    @Transactional
    public void releaseHold(Long holdId, String reason) {
        Hold hold = holdRepository.findByIdWithLock(holdId)
                .orElseThrow(() -> new PaymentException("Hold not found with id: " + holdId));
        
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
}
