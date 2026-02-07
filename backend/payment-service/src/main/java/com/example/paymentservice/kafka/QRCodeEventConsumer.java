package com.example.paymentservice.kafka;

import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.entity.PaymentStatus;
import com.example.paymentservice.repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.annotation.Profile

import java.time.LocalDateTime;

/**
 * Kafka consumer for QR code events in Payment Service
 * Listens to qr.code.generated events to update payment status
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!test")
public class QRCodeEventConsumer {

    private final PaymentRepository paymentRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "qr.code.generated",
            groupId = "payment-service-group",
            containerFactory = "kafkaListenerContainerFactory",
            autoStartup = "true"
    )
    @Transactional
    public void handleQRCodeGenerated(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            Acknowledgment acknowledgment) {
        
        try {
            log.info("Payment Service - Received QRCodeGeneratedEvent: key={}, message={}", key, message);
            
            QRCodeGeneratedEvent event = objectMapper.readValue(message, QRCodeGeneratedEvent.class);
            
            // Update payment status and information (Saga Pattern - Step 4)
            Payment payment = paymentRepository.findById(event.getPaymentId())
                    .orElseThrow(() -> new RuntimeException("Payment not found: " + event.getPaymentId()));
            
            // Update payment status from PENDING to READY (QR code is now available)
            if (payment.getStatus() == PaymentStatus.PENDING) {
                payment.setStatus(PaymentStatus.READY);
                paymentRepository.save(payment);
                
                log.info("Updated payment status to READY for paymentId: {}, qrCodeId: {}", 
                        event.getPaymentId(), event.getQrCodeId());
            } else {
                log.warn("Payment {} is not in PENDING status, current status: {}", 
                        event.getPaymentId(), payment.getStatus());
            }
            
            // Acknowledge message processing
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("Failed to process QRCodeGeneratedEvent: key={}", key, e);
            // In production, you might want to send to a dead letter queue
            // For now, we'll acknowledge to prevent infinite retries
            acknowledgment.acknowledge();
        }
    }

    // Event DTO matching QRCodeEventProducer.QRCodeGeneratedEvent
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class QRCodeGeneratedEvent {
        private Long qrCodeId;
        private Long paymentId;
        private String code;
        private String status;
        private LocalDateTime expiresAt;
        private LocalDateTime createdAt;
        private LocalDateTime timestamp;
    }
}
