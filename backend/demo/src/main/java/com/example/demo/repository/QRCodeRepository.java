package com.example.demo.repository;

import com.example.demo.entity.QRCode;
import com.example.demo.entity.QRCodeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface QRCodeRepository extends JpaRepository<QRCode, Long> {
    
    Optional<QRCode> findByCode(String code);
    
    Optional<QRCode> findByPaymentId(Long paymentId);
    
    List<QRCode> findByStatus(QRCodeStatus status);
    
    List<QRCode> findByStatusAndExpiresAtBefore(QRCodeStatus status, LocalDateTime expiresAt);
    
    /**
     * Find active QR codes for a specific payment
     */
    @Query("SELECT q FROM QRCode q WHERE q.paymentId = :paymentId AND q.status = :status")
    List<QRCode> findActiveByPaymentId(@Param("paymentId") Long paymentId, @Param("status") QRCodeStatus status);
    
    /**
     * Find active QR codes for payments belonging to a specific customer
     * Uses entity name from @Entity annotation
     */
    @Query("SELECT q FROM QRCode q WHERE q.paymentId IN " +
           "(SELECT p.id FROM com.example.demo.entity.Payment p WHERE p.customerId = :customerId) " +
           "AND q.status = :status")
    List<QRCode> findActiveByCustomerId(@Param("customerId") String customerId, @Param("status") QRCodeStatus status);
    
    /**
     * Deactivate all active QR codes for a specific payment
     */
    @Modifying
    @Transactional
    @Query("UPDATE QRCode q SET q.status = :newStatus WHERE q.paymentId = :paymentId AND q.status = :oldStatus")
    int deactivateByPaymentId(@Param("paymentId") Long paymentId, 
                               @Param("oldStatus") QRCodeStatus oldStatus, 
                               @Param("newStatus") QRCodeStatus newStatus);
    
    /**
     * Deactivate all active QR codes for payments belonging to a specific customer
     * Uses fully qualified entity name
     */
    @Modifying
    @Transactional
    @Query("UPDATE QRCode q SET q.status = :newStatus WHERE q.paymentId IN " +
           "(SELECT p.id FROM com.example.demo.entity.Payment p WHERE p.customerId = :customerId) " +
           "AND q.status = :oldStatus")
    int deactivateByCustomerId(@Param("customerId") String customerId,
                                @Param("oldStatus") QRCodeStatus oldStatus,
                                @Param("newStatus") QRCodeStatus newStatus);
    
    /**
     * Find QR codes created after a specific time for a specific user
     */
    @Query("SELECT q FROM QRCode q WHERE q.paymentId IN " +
           "(SELECT p.id FROM com.example.demo.entity.Payment p WHERE p.customerId = :customerId) " +
           "AND q.createdAt >= :after")
    List<QRCode> findByCustomerIdAndCreatedAtAfter(@Param("customerId") String customerId, 
                                                     @Param("after") LocalDateTime after);
}
