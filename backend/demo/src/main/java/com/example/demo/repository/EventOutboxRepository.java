package com.example.demo.repository;

import com.example.demo.entity.EventOutbox;
import com.example.demo.entity.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventOutboxRepository extends JpaRepository<EventOutbox, Long> {
    
    List<EventOutbox> findByStatus(OutboxStatus status);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM EventOutbox e WHERE e.status = :status ORDER BY e.createdAt ASC")
    List<EventOutbox> findPendingEventsWithLock(@Param("status") OutboxStatus status);
    
    @Modifying
    @Query("UPDATE EventOutbox e SET e.status = :newStatus, e.processedAt = :processedAt WHERE e.id = :id")
    int updateStatus(@Param("id") Long id, 
                     @Param("newStatus") OutboxStatus newStatus,
                     @Param("processedAt") LocalDateTime processedAt);
    
    @Modifying
    @Query("UPDATE EventOutbox e SET e.status = :newStatus, e.retryCount = e.retryCount + 1, e.errorMessage = :errorMessage WHERE e.id = :id")
    int updateFailedStatus(@Param("id") Long id,
                          @Param("newStatus") OutboxStatus newStatus,
                          @Param("errorMessage") String errorMessage);
}
