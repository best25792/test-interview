package com.example.paymentservice.kafka;

import com.example.paymentservice.service.PaymentService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Kafka producer for publishing payment events
 * Uses Outbox Pattern: Events are first saved to database, then published to Kafka
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    // Topic names
    public static final String TOPIC_PAYMENT_CREATED = "payment.created";
    public static final String TOPIC_PAYMENT_PROCESSED = "payment.processed";
    public static final String TOPIC_PAYMENT_REFUNDED = "payment.refunded";

    private static final String TRACE_PARENT_HEADER = "traceparent";
    private static final String TRACE_STATE_HEADER = "tracestate";

    /**
     * Publish PaymentCreatedEvent to Kafka
     */
    public void publishPaymentCreated(PaymentService.PaymentCreatedEvent event) {
        publishEvent(TOPIC_PAYMENT_CREATED, event.getPaymentId().toString(), event, null);
    }

    /**
     * Publish PaymentCreatedEvent to Kafka with traceparent from database
     */
    public void publishPaymentCreated(PaymentService.PaymentCreatedEvent event, String traceparent) {
        publishEvent(TOPIC_PAYMENT_CREATED, event.getPaymentId().toString(), event, traceparent);
    }

    /**
     * Publish PaymentProcessedEvent to Kafka
     */
    public void publishPaymentProcessed(PaymentService.PaymentProcessedEvent event) {
        publishEvent(TOPIC_PAYMENT_PROCESSED, event.getPaymentId().toString(), event, null);
    }

    /**
     * Publish PaymentProcessedEvent to Kafka with traceparent from database
     */
    public void publishPaymentProcessed(PaymentService.PaymentProcessedEvent event, String traceparent) {
        publishEvent(TOPIC_PAYMENT_PROCESSED, event.getPaymentId().toString(), event, traceparent);
    }

    /**
     * Publish PaymentRefundedEvent to Kafka
     */
    public void publishPaymentRefunded(PaymentService.PaymentRefundedEvent event) {
        publishEvent(TOPIC_PAYMENT_REFUNDED, event.getPaymentId().toString(), event, null);
    }

    /**
     * Publish PaymentRefundedEvent to Kafka with traceparent from database
     */
    public void publishPaymentRefunded(PaymentService.PaymentRefundedEvent event, String traceparent) {
        publishEvent(TOPIC_PAYMENT_REFUNDED, event.getPaymentId().toString(), event, traceparent);
    }

    /**
     * Generic method to publish events to Kafka with trace context propagation
     * @param topic Kafka topic name
     * @param key Kafka message key
     * @param event Event object to publish
     * @param traceparent Traceparent header from database (if null, extracts from current context)
     */
    private void publishEvent(String topic, String key, Object event, String traceparent) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);

            Map<String, Object> traceHeaders = this.addTraceHeadersFromTraceParent(traceparent);

            // Build message with trace headers
            var message = MessageBuilder.withPayload(eventJson)
                    .setHeader(KafkaHeaders.TOPIC, topic)
                    .setHeader(KafkaHeaders.KEY, key)
                    .copyHeaders(traceHeaders)
                    .build();

            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(message);
            
            future.whenComplete((result, exception) -> {
                if (exception == null) {
                    log.info("Successfully published event to topic: {}, key: {}, offset: {}", 
                            topic, key, result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to publish event to topic: {}, key: {}", topic, key, exception);
                }
            });
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event to JSON for topic: {}, key: {}", topic, key, e);
            throw new RuntimeException("Failed to publish event to Kafka", e);
        }
    }

    /**
     * Create trace headers from stored traceparent header
     * If traceparent is provided, uses it directly (but generates new span ID for child span)
     * If null, extracts from current OpenTelemetry context
     */
    public Map<String, Object> addTraceHeadersFromTraceParent(String traceparent) {
        Map<String, Object> headers = new HashMap<>();

        if (traceparent != null && !traceparent.isEmpty()) {
            // Use traceparent from database, but create a new span ID for the child span
            try {
                // Parse traceparent: 00-{traceId}-{spanId}-{flags}
                String[] parts = traceparent.split("-");
                if (parts.length == 4) {
                    String traceId = parts[1];
                    String oldSpanId = parts[2];
                    String traceFlags = parts[3];
                    
                    // Generate a new span ID for this child span
                    String newSpanId = generateSpanId();
                    
                    // Create new traceparent with same trace ID but new span ID
                    String newTraceParent = String.format("00-%s-%s-%s", traceId, newSpanId, traceFlags);
                    headers.put(TRACE_PARENT_HEADER, newTraceParent);
                    
                    log.debug("Created trace headers from stored traceparent: traceId={}, oldSpanId={}, newSpanId={}", 
                            traceId, oldSpanId, newSpanId);
                } else {
                    // Invalid format, use as-is
                    log.warn("Invalid traceparent format, using as-is: {}", traceparent);
                    headers.put(TRACE_PARENT_HEADER, traceparent);
                }
            } catch (Exception e) {
                log.warn("Failed to parse traceparent, using as-is: {}", e.getMessage());
                headers.put(TRACE_PARENT_HEADER, traceparent);
            }
        } else {
            // No traceparent provided, extract from current context
            try {
                io.opentelemetry.context.Context currentContext = io.opentelemetry.context.Context.current();
                io.opentelemetry.api.trace.Span currentSpan = io.opentelemetry.api.trace.Span.fromContext(currentContext);
                io.opentelemetry.api.trace.SpanContext spanContext = currentSpan.getSpanContext();
                
                if (spanContext.isValid()) {
                    String traceId = spanContext.getTraceId();
                    String spanId = spanContext.getSpanId();
                    String traceFlags = String.format("%02x", spanContext.getTraceFlags().asByte());
                    
                    String traceParent = String.format("00-%s-%s-%s", traceId, spanId, traceFlags);
                    headers.put(TRACE_PARENT_HEADER, traceParent);
                    
                    log.debug("Extracted trace headers from current context: traceId={}, spanId={}", traceId, spanId);
                } else {
                    log.debug("No valid span context found, skipping trace headers");
                }
            } catch (Exception e) {
                log.warn("Failed to extract trace context: {}", e.getMessage());
            }
        }

        return headers;
    }
    
    private static String generateSpanId() {
        java.util.Random random = new java.util.Random();
        StringBuilder sb = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            sb.append(String.format("%x", random.nextInt(16)));
        }
        return sb.toString();
    }
}
