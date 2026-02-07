package com.example.demo.repository.wallet;

import com.example.demo.entity.Hold;
import com.example.demo.entity.HoldStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface HoldRepository extends JpaRepository<Hold, Long> {
    
    Optional<Hold> findByPaymentId(Long paymentId);
    
    List<Hold> findByUserId(Long userId);
    
    List<Hold> findByUserIdAndStatus(Long userId, HoldStatus status);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT h FROM Hold h WHERE h.id = :id")
    Optional<Hold> findByIdWithLock(@Param("id") Long id);
    
    @Query("SELECT COALESCE(SUM(h.amount), 0) FROM Hold h WHERE h.userId = :userId AND h.status = :status")
    BigDecimal getTotalHeldAmountByUserAndStatus(@Param("userId") Long userId, @Param("status") HoldStatus status);
    
    List<Hold> findByStatusAndExpiresAtBefore(HoldStatus status, LocalDateTime expiresAt);
}
