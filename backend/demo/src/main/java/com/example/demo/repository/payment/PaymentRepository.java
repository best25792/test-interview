package com.example.demo.repository.payment;

import com.example.demo.entity.Payment;
import com.example.demo.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    List<Payment> findByStatus(PaymentStatus status);
    
    List<Payment> findByMerchantId(String merchantId);
    
    List<Payment> findByCustomerId(String customerId);
    
    Optional<Payment> findByIdAndStatus(Long id, PaymentStatus status);
}
