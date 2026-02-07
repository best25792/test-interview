package com.example.paymentservice.service;

import com.example.paymentservice.config.MapTextMapGetter;
import com.example.paymentservice.entity.EventOutbox;
import com.example.paymentservice.entity.OutboxStatus;
import com.example.paymentservice.kafka.KafkaEventProducer;
import com.example.paymentservice.repository.EventOutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Processes events from the outbox table
 * Note: In microservices, events can be published to a message queue or processed via REST calls
 * This is a simplified version that can be extended
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxProcessor {

    private final EventOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final KafkaEventProducer kafkaEventProducer;
    private static final Tracer tracer =
            GlobalOpenTelemetry.getTracer("payment-outbox");
    
    private static final int MAX_RETRIES = 3;
    private static final int BATCH_SIZE = 10;

    /**
     * Process pending events from outbox every 2 seconds
     * Publishes events to Kafka using the Outbox Pattern
     */
    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void processOutboxEvents() {
        MDC.put("processType", "polling");
        try {
            // Get pending events with pessimistic lock to prevent concurrent processing
            List<EventOutbox> pendingEvents = outboxRepository.findPendingEventsWithLock(OutboxStatus.PENDING);
            
            if (pendingEvents.isEmpty()) {
                return;
            }
            
            log.debug("Processing {} events from outbox", pendingEvents.size());
            
            // Process events in batches
            int processed = 0;
            for (EventOutbox event : pendingEvents) {
                if (processed >= BATCH_SIZE) {
                    break;
                }
                
                try {
                    processEvent(event);
                    processed++;
                } catch (Exception e) {
                    log.error("Failed to process outbox event: id={}, eventType={}", 
                            event.getId(), event.getEventType(), e);
                    handleFailedEvent(event, e.getMessage());
                }
            }
            
            if (processed > 0) {
                log.debug("Processed {} events from outbox", processed);
            }
        } catch (Exception e) {
            log.error("Error processing outbox events", e);
        } finally {
            MDC.remove("processType");
        }
    }

    private void processEvent(EventOutbox event) throws Exception {
        String traceparent = event.getTraceparent();
        Map<String, String> carrier = Map.of(
                "traceparent", traceparent
        );

        Context parent = GlobalOpenTelemetry.getPropagators()
                .getTextMapPropagator()
                .extract(Context.root(), carrier, MapTextMapGetter.INSTANCE);

        Span span = tracer.spanBuilder("publish-outbox-event")
                .setParent(parent)
                .startSpan();
        
        try (var scope = span.makeCurrent()) {
            // Mark as processing
            outboxRepository.updateStatus(event.getId(), OutboxStatus.PROCESSING, LocalDateTime.now());
            log.info("Processing outbox event: id={}, eventType={}, traceparent={}", 
                    event.getId(), event.getEventType(), traceparent != null ? traceparent : "N/A");
            
            // Publish event to Kafka based on event type, using traceparent from database
            switch (event.getEventType()) {
                case "PaymentCreatedEvent":
                    PaymentService.PaymentCreatedEvent paymentCreated = objectMapper.readValue(
                            event.getEventData(), PaymentService.PaymentCreatedEvent.class);
                    kafkaEventProducer.publishPaymentCreated(paymentCreated, traceparent);
                    break;
                    
                case "PaymentProcessedEvent":
                    PaymentService.PaymentProcessedEvent paymentProcessed = objectMapper.readValue(
                            event.getEventData(), PaymentService.PaymentProcessedEvent.class);
                    kafkaEventProducer.publishPaymentProcessed(paymentProcessed, traceparent);
                    break;
                    
                case "PaymentRefundedEvent":
                    PaymentService.PaymentRefundedEvent paymentRefunded = objectMapper.readValue(
                            event.getEventData(), PaymentService.PaymentRefundedEvent.class);
                    kafkaEventProducer.publishPaymentRefunded(paymentRefunded, traceparent);
                    break;
                    
                default:
                    log.warn("Unknown event type: {}", event.getEventType());
                    throw new IllegalArgumentException("Unknown event type: " + event.getEventType());
            }
            
            // Mark as completed
            outboxRepository.updateStatus(event.getId(), OutboxStatus.COMPLETED, LocalDateTime.now());
            log.debug("Successfully processed outbox event: id={}, eventType={}, traceparent={}", 
                    event.getId(), event.getEventType(), traceparent);
            
        } catch (Exception e) {
            throw e;
        } finally {
            span.end();
            // Clean up MDC
            MDC.remove("traceId");
        }
    }

    private void handleFailedEvent(EventOutbox event, String errorMessage) {
        int newRetryCount = event.getRetryCount() + 1;
        
        if (newRetryCount >= MAX_RETRIES) {
            // Mark as failed after max retries
            outboxRepository.updateFailedStatus(
                    event.getId(), 
                    OutboxStatus.FAILED, 
                    errorMessage
            );
            log.error("Outbox event failed after {} retries: id={}, eventType={}", 
                    MAX_RETRIES, event.getId(), event.getEventType());
        } else {
            // Reset to pending for retry
            outboxRepository.updateStatus(event.getId(), OutboxStatus.PENDING, null);
            log.warn("Outbox event will be retried: id={}, eventType={}, retryCount={}", 
                    event.getId(), event.getEventType(), newRetryCount);
        }
    }
}
