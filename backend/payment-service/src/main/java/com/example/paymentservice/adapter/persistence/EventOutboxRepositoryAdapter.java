package com.example.paymentservice.adapter.persistence;

import com.example.paymentservice.domain.model.EventOutbox;
import com.example.paymentservice.entity.EventOutboxEntity;
import com.example.paymentservice.entity.OutboxStatus;
import com.example.paymentservice.entity.PaymentEntity;
import com.example.paymentservice.mapper.EventOutboxMapper;
import com.example.paymentservice.mapper.TransactionMapper;
import com.example.paymentservice.repository.EventOutboxRepository;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class EventOutboxRepositoryAdapter implements EventOutboxRepository {

    private final SpringDataEventOutboxRepository eventOutboxRepository;
    private final EventOutboxMapper eventOutboxMapper;

    @Override
    public List<EventOutbox> findByStatus(OutboxStatus status) {
        return eventOutboxRepository.findByStatus(status).stream()
                .map(eventOutboxMapper::toDomain)
                .toList();
    }

    @Override
    public List<EventOutbox> findPendingEventsWithLock(OutboxStatus status) {
        return eventOutboxRepository.findPendingEventsWithLock(status).stream()
                .map(eventOutboxMapper::toDomain)
                .toList();
    }

    @Override
    public Integer updateStatus(Long id, OutboxStatus newStatus, LocalDateTime processedAt) {
        return eventOutboxRepository.updateStatus(id, newStatus, processedAt);
    }

    @Override
    public Integer updateFailedStatus(Long id, OutboxStatus newStatus, String errorMessage) {
        return eventOutboxRepository.updateFailedStatus(id, newStatus, errorMessage);
    }
}

interface SpringDataEventOutboxRepository extends JpaRepository<EventOutboxEntity, Long> {

    List<EventOutboxEntity> findByStatus(OutboxStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM EventOutbox e WHERE e.status = :status ORDER BY e.createdAt ASC")
    List<EventOutboxEntity> findPendingEventsWithLock(@Param("status") OutboxStatus status);

    @Modifying
    @Query("UPDATE EventOutbox e SET e.status = :newStatus, e.processedAt = :processedAt WHERE e.id = :id")
    Integer updateStatus(@Param("id") Long id,
                     @Param("newStatus") OutboxStatus newStatus,
                     @Param("processedAt") LocalDateTime processedAt);

    @Modifying
    @Query("UPDATE EventOutbox e SET e.status = :newStatus, e.retryCount = e.retryCount + 1, e.errorMessage = :errorMessage WHERE e.id = :id")
    Integer updateFailedStatus(@Param("id") Long id,
                           @Param("newStatus") OutboxStatus newStatus,
                           @Param("errorMessage") String errorMessage);
}
