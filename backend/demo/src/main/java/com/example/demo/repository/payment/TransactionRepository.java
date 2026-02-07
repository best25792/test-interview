package com.example.demo.repository.payment;

import com.example.demo.entity.Transaction;
import com.example.demo.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    List<Transaction> findByPaymentId(Long paymentId);
    
    List<Transaction> findByType(TransactionType type);
    
    List<Transaction> findByPaymentIdOrderByTimestampDesc(Long paymentId);
}
