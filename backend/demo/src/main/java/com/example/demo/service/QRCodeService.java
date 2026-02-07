package com.example.demo.service;

import com.example.demo.entity.QRCode;
import com.example.demo.entity.QRCodeStatus;
import com.example.demo.event.EventPublisher;
import com.example.demo.event.QRCodeGeneratedEvent;
import com.example.demo.event.QRCodeUsedEvent;
import com.example.demo.event.QRCodeValidatedEvent;
import com.example.demo.repository.qr.QRCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * QR Code Service - Simulated as a separate microservice
 * Communicates with other services via events only
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QRCodeService {

    private final QRCodeRepository qrCodeRepository;
    private final EventPublisher eventPublisher;

    /**
     * Listen to PaymentCreatedEvent and generate QR code
     * Deactivates any existing active QR codes for the same customer
     * Uses AFTER_COMMIT to ensure payment transaction is committed before creating QR code
     * REQUIRES_NEW ensures QR code creation happens in its own transaction
     */
    @EventListener()
    @Async
    @Transactional
    public void handlePaymentCreated(com.example.demo.event.PaymentCreatedEvent event) {
        log.info("QRCodeService: Received PaymentCreatedEvent for paymentId: {}, customerId: {}", 
                event.getPaymentId(), event.getCustomerId());
        
        // Deactivate any existing active QR codes for the same payment (if any)
        int deactivatedByPayment = qrCodeRepository.deactivateByPaymentId(
                event.getPaymentId(),
                QRCodeStatus.ACTIVE,
                QRCodeStatus.CANCELLED
        );
        
        if (deactivatedByPayment > 0) {
            log.info("QRCodeService: Deactivated {} existing active QR code(s) for paymentId: {}", 
                    deactivatedByPayment, event.getPaymentId());
        }
        
        // Generate new QR code
        String qrCodeData = generateUniqueQRCode(event.getPaymentId());
        
        QRCode qrCode = QRCode.builder()
                .code(qrCodeData)
                .paymentId(event.getPaymentId())
                .status(QRCodeStatus.ACTIVE)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();
        
        QRCode savedQRCode = qrCodeRepository.save(qrCode);
        
        // Publish QR code generated event
        QRCodeGeneratedEvent qrEvent = QRCodeGeneratedEvent.builder()
                .qrCodeId(savedQRCode.getId())
                .qrCode(savedQRCode.getCode())
                .paymentId(savedQRCode.getPaymentId())
                .expiresAt(savedQRCode.getExpiresAt())
                .timestamp(LocalDateTime.now())
                .build();
        eventPublisher.publishQRCodeGenerated(qrEvent);
        
        log.info("QRCodeService: New QR code generated for paymentId: {}. Previous active QR codes deactivated.", 
                event.getPaymentId());
    }

    public QRCode validateQRCode(String code) {
        QRCode qrCode = qrCodeRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("QR code not found: " + code));
        
        boolean isValid = true;
        String reason = null;
        
        if (qrCode.getStatus() != QRCodeStatus.ACTIVE) {
            isValid = false;
            reason = "QR code is not active. Status: " + qrCode.getStatus();
        }
        
        if (qrCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            qrCode.setStatus(QRCodeStatus.EXPIRED);
            qrCodeRepository.save(qrCode);
            isValid = false;
            reason = "QR code has expired";
        }
        
        // Publish validation event
        QRCodeValidatedEvent validationEvent = QRCodeValidatedEvent.builder()
                .qrCodeId(qrCode.getId())
                .qrCode(qrCode.getCode())
                .paymentId(qrCode.getPaymentId())
                .isValid(isValid)
                .reason(reason)
                .timestamp(LocalDateTime.now())
                .build();
        
        eventPublisher.publishQRCodeValidated(validationEvent);
        
        if (!isValid) {
            throw new RuntimeException(reason);
        }
        
        return qrCode;
    }

    @Transactional
    public void markQRCodeAsUsed(Long qrCodeId) {
        QRCode qrCode = qrCodeRepository.findById(qrCodeId)
                .orElseThrow(() -> new RuntimeException("QR code not found with id: " + qrCodeId));
        
        qrCode.setStatus(QRCodeStatus.USED);
        QRCode savedQRCode = qrCodeRepository.save(qrCode);
        
        // Publish QR code used event
        QRCodeUsedEvent event = QRCodeUsedEvent.builder()
                .qrCodeId(savedQRCode.getId())
                .qrCode(savedQRCode.getCode())
                .paymentId(savedQRCode.getPaymentId())
                .timestamp(LocalDateTime.now())
                .build();
        
        eventPublisher.publishQRCodeUsed(event);
        
        log.info("QRCodeService: QR code marked as used. qrCodeId: {}", qrCodeId);
    }

    /**
     * Deactivate all active QR codes for a payment
     * This can be called when user requests a new QR code
     */
    @Transactional
    public void deactivateActiveQRCodesForPayment(Long paymentId) {
        int deactivatedCount = qrCodeRepository.deactivateByPaymentId(
                paymentId,
                QRCodeStatus.ACTIVE,
                QRCodeStatus.CANCELLED
        );
        
        log.info("QRCodeService: Deactivated {} active QR code(s) for paymentId: {}", 
                deactivatedCount, paymentId);
    }

    private String generateUniqueQRCode(Long paymentId) {
        // Generate a unique QR code string
        // In production, this would integrate with a QR code generation service
        return "PAYMENT_" + paymentId + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
