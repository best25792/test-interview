package com.example.paymentservice.repository;

import com.example.paymentservice.domain.model.EventOutbox;
import com.example.paymentservice.entity.OutboxStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface EventOutboxRepository {
    
    List<EventOutbox> findByStatus(OutboxStatus status);

    List<EventOutbox> findPendingEventsWithLock(OutboxStatus status);

    Integer updateStatus(Long id ,OutboxStatus newStatus ,LocalDateTime processedAt);

    Integer updateFailedStatus(Long id ,OutboxStatus newStatus ,String errorMessage);
}
