package com.example.demo.controller;

import com.example.demo.dto.response.TransactionResponse;
import com.example.demo.entity.Transaction;
import com.example.demo.entity.TransactionType;
import com.example.demo.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getTransaction(@PathVariable Long id) {
        Transaction transaction = transactionService.getTransactionById(id);
        TransactionResponse response = mapToTransactionResponse(transaction);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getAllTransactions(
            @RequestParam(required = false) Long paymentId,
            @RequestParam(required = false) TransactionType type) {
        
        List<Transaction> transactions;
        
        if (paymentId != null) {
            transactions = transactionService.getTransactionsByPaymentId(paymentId);
        } else if (type != null) {
            transactions = transactionService.getTransactionsByType(type);
        } else {
            transactions = transactionService.getAllTransactions();
        }
        
        List<TransactionResponse> responses = transactions.stream()
                .map(this::mapToTransactionResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }

    private TransactionResponse mapToTransactionResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .paymentId(transaction.getPaymentId())
                .type(transaction.getType())
                .amount(transaction.getAmount())
                .status(transaction.getStatus())
                .reference(transaction.getReference())
                .externalReference(transaction.getExternalReference())
                .timestamp(transaction.getTimestamp())
                .description(transaction.getDescription())
                .build();
    }
}
