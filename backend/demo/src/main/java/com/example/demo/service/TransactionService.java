package com.example.demo.service;

import com.example.demo.entity.Transaction;
import com.example.demo.entity.TransactionType;
import com.example.demo.exception.PaymentNotFoundException;
import com.example.demo.repository.payment.PaymentRepository;
import com.example.demo.repository.payment.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final PaymentRepository paymentRepository;

    public Transaction getTransactionById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found with id: " + id));
    }

    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    public List<Transaction> getTransactionsByPaymentId(Long paymentId) {
        // Validate payment exists
        paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        
        return transactionRepository.findByPaymentIdOrderByTimestampDesc(paymentId);
    }

    public List<Transaction> getTransactionsByType(TransactionType type) {
        return transactionRepository.findByType(type);
    }
}
