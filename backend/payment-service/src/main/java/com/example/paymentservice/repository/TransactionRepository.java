package com.example.paymentservice.repository;

import aj.org.objectweb.asm.commons.Remapper;
import com.example.paymentservice.domain.model.Transaction;
import com.example.paymentservice.entity.TransactionType;


import java.util.List;
import java.util.Optional;

public interface TransactionRepository  {
    
    List<Transaction> findByPaymentId(Long paymentId);
    
    List<Transaction> findByType(TransactionType type);
    
    List<Transaction> findByPaymentIdOrderByTimestampDesc(Long paymentId);

    List<Transaction> findAll();

    Optional<Transaction> findById(Long id);
}
