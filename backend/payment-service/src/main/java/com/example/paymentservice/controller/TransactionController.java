package com.example.paymentservice.controller;

import com.example.paymentservice.domain.model.Transaction;
import com.example.paymentservice.dto.response.TransactionResponse;
import com.example.paymentservice.entity.TransactionType;
import com.example.paymentservice.mapper.TransactionMapper;
import com.example.paymentservice.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionMapper transactionMapper;

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getTransaction(@PathVariable Long id) {
        Transaction transaction = transactionService.getTransactionById(id);
        return ResponseEntity.ok(transactionMapper.toResponse(transaction));
    }

    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getAllTransactions(
            @RequestParam(required = false) Long paymentId,
            @RequestParam(required = false) TransactionType type) {

        List<Transaction> transactions = paymentId != null
                ? transactionService.getTransactionsByPaymentId(paymentId)
                : type != null
                ? transactionService.getTransactionsByType(type)
                : transactionService.getAllTransactions();

        List<TransactionResponse> responses = transactions.stream()
                .map(transactionMapper::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }
}
