package com.example.paymentservice.mapper;

import com.example.paymentservice.domain.model.EventOutbox;
import com.example.paymentservice.entity.EventOutboxEntity;
import org.springframework.stereotype.Component;

@Component
public class EventOutboxMapper {

    public EventOutbox toDomain(EventOutboxEntity entity) {
        if (entity == null) return null;
        return EventOutbox.builder()
                .id(entity.getId())
                .eventType(entity.getEventType())
                .eventData(entity.getEventData())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .processedAt(entity.getProcessedAt())
                .retryCount(entity.getRetryCount())
                .errorMessage(entity.getErrorMessage())
                .traceparent(entity.getTraceparent())
                .build();
    }

    /** Map domain to JPA entity (for save/update). */
    public EventOutboxEntity toEntity(EventOutbox domain) {
        if (domain == null) return null;
        return EventOutboxEntity.builder()
                .id(domain.getId())
                .eventType(domain.getEventType())
                .eventData(domain.getEventData())
                .status(domain.getStatus())
                .createdAt(domain.getCreatedAt())
                .processedAt(domain.getProcessedAt())
                .retryCount(domain.getRetryCount())
                .errorMessage(domain.getErrorMessage())
                .traceparent(domain.getTraceparent())
                .build();
    }
}
