package com.example.paymentservice.service;

import com.example.paymentservice.config.MapTextMapGetter;
import com.example.paymentservice.domain.model.EventOutbox;
import com.example.paymentservice.entity.OutboxStatus;
import com.example.paymentservice.repository.EventOutboxRepository;
import com.example.paymentservice.service.outbox.OutboxEventConsumer;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private final List<OutboxEventConsumer> outboxEventConsumers;

    private static final Tracer tracer = GlobalOpenTelemetry.getTracer("payment-outbox");
    private static final int MAX_RETRIES = 3;
    private static final int BATCH_SIZE = 10;

    /** Registry: event type -> consumer. Built at startup from injected consumers. */
    private Map<String, OutboxEventConsumer> consumerRegistry;

    @PostConstruct
    void initConsumerRegistry() {
        consumerRegistry = outboxEventConsumers.stream()
                .collect(Collectors.toMap(OutboxEventConsumer::getEventType, c -> c));
        log.debug("Registered {} outbox event consumer(s): {}", consumerRegistry.size(), consumerRegistry.keySet());
    }

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
            
            // Dispatch to the consumer registered for this event type
            OutboxEventConsumer consumer = consumerRegistry.get(event.getEventType());
            if (consumer == null) {
                log.warn("Unknown event type: {}", event.getEventType());
                throw new IllegalArgumentException("Unknown event type: " + event.getEventType());
            }
            consumer.consume(event.getEventData(), traceparent);
            
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
