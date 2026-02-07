package com.example.qrservice.kafka;

import com.example.qrservice.entity.QRCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka producer for QR code events
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QRCodeEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    // Topic name
    public static final String TOPIC_QR_CODE_GENERATED = "qr.code.generated";

    /**
     * Publish QRCodeGeneratedEvent to Kafka
     */
    public void publishQRCodeGenerated(QRCode qrCode) {
        QRCodeGeneratedEvent event = QRCodeGeneratedEvent.builder()
                .qrCodeId(qrCode.getId())
                .paymentId(qrCode.getPaymentId())
                .code(qrCode.getCode())
                .status(qrCode.getStatus().toString())
                .expiresAt(qrCode.getExpiresAt())
                .createdAt(qrCode.getCreatedAt())
                .timestamp(java.time.LocalDateTime.now())
                .build();

        publishEvent(TOPIC_QR_CODE_GENERATED, qrCode.getPaymentId().toString(), event);
    }

    /**
     * Generic method to publish events to Kafka
     */
    private void publishEvent(String topic, String key, Object event) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(topic, key, eventJson);
            
            future.whenComplete((result, exception) -> {
                if (exception == null) {
                    log.info("Successfully published event to topic: {}, key: {}, offset: {}", 
                            topic, key, result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to publish event to topic: {}, key: {}", topic, key, exception);
                }
            });
        } catch (Exception e) {
            log.error("Failed to serialize event to JSON for topic: {}, key: {}", topic, key, e);
            throw new RuntimeException("Failed to publish event to Kafka", e);
        }
    }

    // Event DTO
    @lombok.Builder
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class QRCodeGeneratedEvent {
        private Long qrCodeId;
        private Long paymentId;
        private String code;
        private String status;
        private java.time.LocalDateTime expiresAt;
        private java.time.LocalDateTime createdAt;
        private java.time.LocalDateTime timestamp;
    }
}
