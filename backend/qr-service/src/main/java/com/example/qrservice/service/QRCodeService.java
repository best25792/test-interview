package com.example.qrservice.service;

import com.example.qrservice.entity.QRCode;
import com.example.qrservice.entity.QRCodeStatus;
import com.example.qrservice.repository.QRCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * QR Code Service - Manages QR code generation and validation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QRCodeService {

    private final QRCodeRepository qrCodeRepository;

    /**
     * Create QR code for a payment
     */
    @Transactional
    public QRCode createQRCode(Long paymentId, String customerId) {
        // Deactivate any existing active QR codes for the same payment
        int deactivatedByPayment = qrCodeRepository.deactivateByPaymentId(
                paymentId,
                QRCodeStatus.ACTIVE,
                QRCodeStatus.CANCELLED
        );
        
        if (deactivatedByPayment > 0) {
            log.info("Deactivated {} existing active QR code(s) for paymentId: {}", 
                    deactivatedByPayment, paymentId);
        }
        
        // Generate new QR code
        String qrCodeData = generateUniqueQRCode(paymentId);
        
        QRCode qrCode = QRCode.builder()
                .code(qrCodeData)
                .paymentId(paymentId)
                .status(QRCodeStatus.ACTIVE)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();
        
        QRCode savedQRCode = qrCodeRepository.save(qrCode);
        
        log.info("New QR code generated for paymentId: {}", paymentId);
        return savedQRCode;
    }

    /**
     * Validate QR code
     */
    @Transactional
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
        
        if (!isValid) {
            throw new RuntimeException(reason);
        }
        
        return qrCode;
    }

    /**
     * Mark QR code as used
     */
    @Transactional
    public void markQRCodeAsUsed(Long qrCodeId) {
        QRCode qrCode = qrCodeRepository.findById(qrCodeId)
                .orElseThrow(() -> new RuntimeException("QR code not found with id: " + qrCodeId));
        
        qrCode.setStatus(QRCodeStatus.USED);
        qrCodeRepository.save(qrCode);
        
        log.info("QR code marked as used. qrCodeId: {}", qrCodeId);
    }

    /**
     * Deactivate all active QR codes for a payment
     */
    @Transactional
    public void deactivateActiveQRCodesForPayment(Long paymentId) {
        int deactivatedCount = qrCodeRepository.deactivateByPaymentId(
                paymentId,
                QRCodeStatus.ACTIVE,
                QRCodeStatus.CANCELLED
        );
        
        log.info("Deactivated {} active QR code(s) for paymentId: {}", 
                deactivatedCount, paymentId);
    }

    private String generateUniqueQRCode(Long paymentId) {
        // Generate a unique QR code string with timestamp and UUID for better uniqueness
        long timestamp = System.currentTimeMillis();
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return "PAYMENT_" + paymentId + "_" + timestamp + "_" + uuid.substring(0, 16);
    }
}
