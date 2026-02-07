package com.example.paymentservice.repository;

import com.example.paymentservice.entity.Transaction;
import com.example.paymentservice.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    List<Transaction> findByPaymentId(Long paymentId);
    
    List<Transaction> findByType(TransactionType type);
    
    List<Transaction> findByPaymentIdOrderByTimestampDesc(Long paymentId);
}
