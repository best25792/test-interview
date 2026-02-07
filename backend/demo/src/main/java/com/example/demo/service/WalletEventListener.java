package com.example.demo.service;

import com.example.demo.event.WalletDeductedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Wallet Service Event Listener
 * Handles wallet-related events
 * In a real microservices architecture, this would be in a separate wallet service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WalletEventListener {

    /**
     * Handle wallet deduction event - can be used for notifications, audit logs, etc.
     */
    @EventListener
    @Async
    public void handleWalletDeducted(WalletDeductedEvent event) {
        log.info("WalletService: Wallet deducted event received. UserId: {}, PaymentId: {}, Amount: {}, " +
                "Old Balance: {}, New Balance: {}", 
                event.getUserId(), event.getPaymentId(), event.getAmount(), 
                event.getOldBalance(), event.getNewBalance());
        
        // In a real system, you might:
        // - Send notification to user
        // - Update audit logs
        // - Trigger fraud detection
        // - Update analytics
    }
}
