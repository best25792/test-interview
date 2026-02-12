package com.example.paymentservice.repository;

import com.example.paymentservice.domain.model.Payment;
import com.example.paymentservice.entity.PaymentStatus;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository {
    
    List<Payment> findByStatus(PaymentStatus status);
    
    List<Payment> findByMerchantId(String merchantId);
    
    List<Payment> findByCustomerId(String customerId);
    
    Optional<Payment> findByIdAndStatus(Long id, PaymentStatus status);

    Optional<Payment> findById(Long id);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    List<Payment> findAll();

    Payment save(Payment payment);

    Boolean existsById(Long id);
}
