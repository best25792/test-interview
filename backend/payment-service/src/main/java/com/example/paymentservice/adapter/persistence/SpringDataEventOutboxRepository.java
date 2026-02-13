package com.example.paymentservice.adapter.persistence;

import com.example.paymentservice.entity.EventOutboxEntity;
import com.example.paymentservice.entity.OutboxStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SpringDataEventOutboxRepository extends JpaRepository<EventOutboxEntity, Long> {

    List<EventOutboxEntity> findByStatus(OutboxStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM EventOutboxEntity e WHERE e.status = :status ORDER BY e.createdAt ASC")
    List<EventOutboxEntity> findPendingEventsWithLock(@Param("status") OutboxStatus status);

    @Modifying
    @Query("UPDATE EventOutboxEntity e SET e.status = :newStatus, e.processedAt = :processedAt WHERE e.id = :id")
    Integer updateStatus(@Param("id") Long id,
                     @Param("newStatus") OutboxStatus newStatus,
                     @Param("processedAt") LocalDateTime processedAt);

    @Modifying
    @Query("UPDATE EventOutboxEntity e SET e.status = :newStatus, e.retryCount = e.retryCount + 1, e.errorMessage = :errorMessage WHERE e.id = :id")
    Integer updateFailedStatus(@Param("id") Long id,
                           @Param("newStatus") OutboxStatus newStatus,
                           @Param("errorMessage") String errorMessage);
}
