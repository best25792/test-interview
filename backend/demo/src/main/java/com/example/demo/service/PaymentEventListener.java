package com.example.demo.service;

import com.example.demo.entity.PaymentStatus;
import com.example.demo.event.PaymentProcessedEvent;
import com.example.demo.event.QRCodeValidatedEvent;
import com.example.demo.event.QRCodeUsedEvent;
import com.example.demo.repository.payment.PaymentRepository;
import com.example.demo.repository.qr.QRCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Payment Service Event Listener
 * Handles events related to payment processing
 * In a real microservices architecture, this would be in a separate service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentEventListener {

    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

    /**
     * Handle QR code used event - payment has been processed
     */
    @EventListener
    @Async
    public void handleQRCodeUsed(QRCodeUsedEvent event) {
        log.info("PaymentService: QR code used event received. qrCodeId: {}, paymentId: {}", 
                event.getQrCodeId(), event.getPaymentId());
        
        // In a real async system, we'd update payment status here
        // For now, this is handled synchronously in processPayment
    }
}
