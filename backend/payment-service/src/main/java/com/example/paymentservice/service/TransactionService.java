package com.example.paymentservice.service;

import com.example.paymentservice.domain.model.Transaction;
import com.example.paymentservice.entity.TransactionType;
import com.example.paymentservice.exception.PaymentNotFoundException;
import com.example.paymentservice.mapper.TransactionMapper;
import com.example.paymentservice.repository.PaymentRepository;
import com.example.paymentservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final PaymentRepository paymentRepository;
    private final TransactionMapper transactionMapper;

    public Transaction getTransactionById(Long id) {
        return transactionRepository.findById(id)
                .map(transactionMapper::toDomain)
                .orElseThrow(() -> new RuntimeException("Transaction not found with id: " + id));
    }

    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll().stream()
                .map(transactionMapper::toDomain)
                .toList();
    }

    public List<Transaction> getTransactionsByPaymentId(Long paymentId) {
        paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        return transactionRepository.findByPaymentIdOrderByTimestampDesc(paymentId).stream()
                .map(transactionMapper::toDomain)
                .toList();
    }

    public List<Transaction> getTransactionsByType(TransactionType type) {
        return transactionRepository.findByType(type).stream()
                .map(transactionMapper::toDomain)
                .toList();
    }
}
