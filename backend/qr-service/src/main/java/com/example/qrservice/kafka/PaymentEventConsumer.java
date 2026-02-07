package com.example.qrservice.kafka;

import com.example.qrservice.entity.QRCode;
import com.example.qrservice.service.QRCodeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for payment events in QR Service
 * Listens to payment.created events to generate QR codes
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final QRCodeService qrCodeService;
    private final ObjectMapper objectMapper;
    private final QRCodeEventProducer qrCodeEventProducer;

    @KafkaListener(
            topics = "payment.created",
            groupId = "qr-service-group",
            containerFactory = "kafkaListenerContainerFactory",
            autoStartup = "true"
    )
    public void handlePaymentCreated(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            Acknowledgment acknowledgment) {
        
        try {
            log.info("QR Service - Received PaymentCreatedEvent: key={}, message={}", key, message);
            
            PaymentCreatedEvent event = objectMapper.readValue(message, PaymentCreatedEvent.class);
            
            // Generate QR code for the payment (Saga Pattern - Step 2)
            // Note: This is idempotent - if QR code already exists (created via REST), 
            // createQRCode will deactivate old ones and create a new one
            QRCode qrCode = qrCodeService.createQRCode(event.getPaymentId(), event.getCustomerId());
            
            log.info("Successfully generated QR code for paymentId: {}", event.getPaymentId());
            
            // Publish QRCodeGeneratedEvent to notify Payment Service (Saga Pattern - Step 3)
            qrCodeEventProducer.publishQRCodeGenerated(qrCode);
            
            log.info("Published QRCodeGeneratedEvent for paymentId: {}", event.getPaymentId());
            
            // Acknowledge message processing
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("Failed to process PaymentCreatedEvent: key={}", key, e);
            // In production, you might want to send to a dead letter queue
            // For now, we'll acknowledge to prevent infinite retries
            acknowledgment.acknowledge();
        }
    }

    // Event DTO matching PaymentService.PaymentCreatedEvent
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PaymentCreatedEvent {
        private Long paymentId;
        private java.math.BigDecimal amount;
        private String currency;
        private String merchantId;
        private String customerId;
        private String description;
        private java.time.LocalDateTime timestamp;
    }
}
