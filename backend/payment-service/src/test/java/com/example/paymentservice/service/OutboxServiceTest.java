package com.example.paymentservice.service;

import com.example.paymentservice.entity.EventOutbox;
import com.example.paymentservice.entity.OutboxStatus;
import com.example.paymentservice.repository.EventOutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {

    @Mock
    private EventOutboxRepository outboxRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OutboxService outboxService;

    private TestEvent testEvent;

    @BeforeEach
    void setUp() {
        testEvent = new TestEvent(1L, "Test Event", LocalDateTime.now());
    }

    @Test
    void testSaveEvent_Success() throws Exception {
        // Given
        String eventType = "TestEvent";
        String eventData = "{\"id\":1,\"name\":\"Test Event\"}";
        
        EventOutbox savedOutbox = EventOutbox.builder()
                .id(1L)
                .eventType(eventType)
                .eventData(eventData)
                .status(OutboxStatus.PENDING)
                .build();

        when(objectMapper.writeValueAsString(testEvent)).thenReturn(eventData);
        when(outboxRepository.save(any(EventOutbox.class))).thenReturn(savedOutbox);

        // When
        outboxService.saveEvent(eventType, testEvent);

        // Then
        verify(objectMapper).writeValueAsString(testEvent);
        verify(outboxRepository).save(any(EventOutbox.class));
    }

    @Test
    void testSaveEvent_JsonProcessingException() throws Exception {
        // Given
        String eventType = "TestEvent";
        when(objectMapper.writeValueAsString(testEvent))
                .thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("JSON error") {});

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            outboxService.saveEvent(eventType, testEvent);
        });

        assertTrue(exception.getMessage().contains("Failed to save event to outbox"));
        verify(outboxRepository, never()).save(any(EventOutbox.class));
    }

    // Test event class
    static class TestEvent {
        private Long id;
        private String name;
        private LocalDateTime timestamp;

        public TestEvent(Long id, String name, LocalDateTime timestamp) {
            this.id = id;
            this.name = name;
            this.timestamp = timestamp;
        }

        public Long getId() { return id; }
        public String getName() { return name; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
}
