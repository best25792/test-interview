package com.example.demo.service;

import com.example.demo.entity.EventOutbox;
import com.example.demo.entity.OutboxStatus;
import com.example.demo.repository.payment.EventOutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
     */
    @Transactional
    public void saveEvent(String eventType, Object event) {
        try {
            String eventData = objectMapper.writeValueAsString(event);
            
            EventOutbox outbox = EventOutbox.builder()
                    .eventType(eventType)
                    .eventData(eventData)
                    .status(OutboxStatus.PENDING)
                    .build();
            
            outboxRepository.save(outbox);
            log.debug("Event saved to outbox: eventType={}, outboxId={}", eventType, outbox.getId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event to JSON: eventType={}", eventType, e);
            throw new RuntimeException("Failed to save event to outbox", e);
        }
    }
}
