package com.example.demo.service;

import com.example.demo.entity.EventOutbox;
import com.example.demo.entity.OutboxStatus;
import com.example.demo.event.*;
import com.example.demo.repository.payment.EventOutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Processes events from the outbox table and publishes them
 * This ensures events are only published after the transaction commits
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxProcessor {

    private final EventOutboxRepository outboxRepository;
    private final EventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    
    private static final int MAX_RETRIES = 3;
    private static final int BATCH_SIZE = 10;

    /**
     * Process pending events from outbox every 2 seconds
     */
//    @Scheduled(fixedDelay = 2000)
//    @Transactional
    public void processOutboxEvents() {

        MDC.put("processType", "polling"); // Tag logs for filtering
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
            MDC.remove("processType"); // Clean up after task
        }
    }

    private void processEvent(EventOutbox event) throws Exception {
        // Mark as processing
        outboxRepository.updateStatus(event.getId(), OutboxStatus.PROCESSING, LocalDateTime.now());
        
        try {
            // Deserialize and publish event based on type
            switch (event.getEventType()) {
                case "PaymentCreatedEvent":
                    PaymentCreatedEvent paymentCreated = objectMapper.readValue(
                            event.getEventData(), PaymentCreatedEvent.class);
                    eventPublisher.publishPaymentCreated(paymentCreated);
                    break;
                    
                case "PaymentProcessedEvent":
                    PaymentProcessedEvent paymentProcessed = objectMapper.readValue(
                            event.getEventData(), PaymentProcessedEvent.class);
                    eventPublisher.publishPaymentProcessed(paymentProcessed);
                    break;
                    
                case "PaymentConfirmedEvent":
                    PaymentConfirmedEvent paymentConfirmed = objectMapper.readValue(
                            event.getEventData(), PaymentConfirmedEvent.class);
                    eventPublisher.publishPaymentConfirmed(paymentConfirmed);
                    break;
                    
                case "PaymentRefundedEvent":
                    PaymentRefundedEvent paymentRefunded = objectMapper.readValue(
                            event.getEventData(), PaymentRefundedEvent.class);
                    eventPublisher.publishPaymentRefunded(paymentRefunded);
                    break;
                    
                default:
                    log.warn("Unknown event type: {}", event.getEventType());
                    throw new IllegalArgumentException("Unknown event type: " + event.getEventType());
            }
            
            // Mark as completed
            outboxRepository.updateStatus(event.getId(), OutboxStatus.COMPLETED, LocalDateTime.now());
            log.debug("Successfully processed outbox event: id={}, eventType={}", 
                    event.getId(), event.getEventType());
            
        } catch (Exception e) {
            throw e; // Re-throw to be handled by caller
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
