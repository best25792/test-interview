package com.example.walletservice.kafka;

import com.example.walletservice.service.HoldService;
import com.example.walletservice.service.WalletService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Kafka consumer for payment events in Wallet Service
 * Handles payment processing, confirmation, and refund events
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final HoldService holdService;
    private final WalletService walletService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "payment.processed",
            groupId = "wallet-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePaymentProcessed(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            Acknowledgment acknowledgment) {
        
        try {
            log.info("Received PaymentProcessedEvent: key={}", key);
            
            PaymentProcessedEvent event = objectMapper.readValue(message, PaymentProcessedEvent.class);
            
            // Create hold for the payment (already done via REST call, but this is for event-driven consistency)
            // In a fully event-driven architecture, this would create the hold
            log.info("Payment processed event received for paymentId: {}, amount: {}", 
                    event.getPaymentId(), event.getAmount());
            
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("Failed to process PaymentProcessedEvent: key={}", key, e);
            acknowledgment.acknowledge();
        }
    }

    @KafkaListener(
            topics = "payment.confirmed",
            groupId = "wallet-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePaymentConfirmed(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            Acknowledgment acknowledgment) {
        
        try {
            log.info("Received PaymentConfirmedEvent: key={}", key);
            
            PaymentConfirmedEvent event = objectMapper.readValue(message, PaymentConfirmedEvent.class);
            
            // Hold capture is already done via REST call, but this confirms the event
            log.info("Payment confirmed event received for paymentId: {}, amount: {}", 
                    event.getPaymentId(), event.getAmount());
            
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("Failed to process PaymentConfirmedEvent: key={}", key, e);
            acknowledgment.acknowledge();
        }
    }

    @KafkaListener(
            topics = "payment.refunded",
            groupId = "wallet-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePaymentRefunded(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            Acknowledgment acknowledgment) {
        
        try {
            log.info("Received PaymentRefundedEvent: key={}", key);
            
            PaymentRefundedEvent event = objectMapper.readValue(message, PaymentRefundedEvent.class);
            
            // Refund is already processed via REST call, but this confirms the event
            log.info("Payment refunded event received for paymentId: {}, refundAmount: {}", 
                    event.getPaymentId(), event.getRefundAmount());
            
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("Failed to process PaymentRefundedEvent: key={}", key, e);
            acknowledgment.acknowledge();
        }
    }

    // Event DTOs
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PaymentProcessedEvent {
        private Long paymentId;
        private String qrCode;
        private BigDecimal amount;
        private String currency;
        private java.time.LocalDateTime timestamp;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PaymentConfirmedEvent {
        private Long paymentId;
        private BigDecimal amount;
        private String currency;
        private java.time.LocalDateTime timestamp;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PaymentRefundedEvent {
        private Long paymentId;
        private BigDecimal refundAmount;
        private String reason;
        private java.time.LocalDateTime timestamp;
    }
}
