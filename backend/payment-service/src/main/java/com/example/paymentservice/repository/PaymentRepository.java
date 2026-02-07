package com.example.paymentservice.repository;

import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    List<Payment> findByStatus(PaymentStatus status);
    
    List<Payment> findByMerchantId(String merchantId);
    
    List<Payment> findByCustomerId(String customerId);
    
    Optional<Payment> findByIdAndStatus(Long id, PaymentStatus status);
    
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
}
