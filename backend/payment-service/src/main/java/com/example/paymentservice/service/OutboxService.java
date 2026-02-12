package com.example.paymentservice.service;

import com.example.paymentservice.domain.model.EventOutbox;
import com.example.paymentservice.entity.OutboxStatus;
import com.example.paymentservice.repository.EventOutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxService {

    private final EventOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    /**
     * Save event to outbox table within the same transaction
     * Extracts and stores traceparent header from current OpenTelemetry context
     */
    @Transactional
    public void saveEvent(String eventType, Object event) {
        try {
            String eventData = objectMapper.writeValueAsString(event);

            // Extract traceparent header from current OpenTelemetry context
            String traceparent = extractTraceParent();

            EventOutbox outbox = EventOutbox.builder()
                    .eventType(eventType)
                    .eventData(eventData)
                    .status(OutboxStatus.PENDING)
                    .traceparent(traceparent)
                    .build();

            EventOutbox saved = outboxRepository.save(outbox);
            log.debug("Event saved to outbox: eventType={}, outboxId={}, traceparent={}",
                    eventType, saved.getId(), traceparent);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event to JSON: eventType={}", eventType, e);
            throw new RuntimeException("Failed to save event to outbox", e);
        }
    }

    /**
     * Extract traceparent header from current OpenTelemetry context
     * Format: 00-{traceId}-{spanId}-{flags}
     */
    private String extractTraceParent() {
        try {
            Context currentContext = Context.current();
            Span currentSpan = Span.fromContext(currentContext);
            SpanContext spanContext = currentSpan.getSpanContext();
            
            if (spanContext.isValid()) {
                // Format: traceparent = 00-{traceId}-{spanId}-{flags}
                String traceId = spanContext.getTraceId();
                String spanId = spanContext.getSpanId();
                String traceFlags = String.format("%02x", spanContext.getTraceFlags().asByte());
                
                String traceparent = String.format("00-%s-%s-%s", traceId, spanId, traceFlags);
                log.debug("Extracted traceparent: {}", traceparent);
                return traceparent;
            } else {
                log.debug("No valid span context found, traceparent will be null");
                return null;
            }
        } catch (Exception e) {
            log.warn("Failed to extract traceparent: {}", e.getMessage());
            return null;
        }
    }
}
