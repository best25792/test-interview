package com.example.paymentservice.service;

import com.example.paymentservice.domain.model.Transaction;
import com.example.paymentservice.entity.TransactionStatus;
import com.example.paymentservice.entity.TransactionType;
import com.example.paymentservice.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    private final Long transactionId = 1L;
    private final Long paymentId = 100L;
    private Transaction domainTransaction;

    @BeforeEach
    void setUp() {
        domainTransaction = Transaction.builder()
                .id(transactionId)
                .paymentId(paymentId)
                .type(TransactionType.PAYMENT)
                .amount(new BigDecimal("50.00"))
                .status(TransactionStatus.SUCCESS)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Test
    void testGetTransactionById_Success() {
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(domainTransaction));

        Transaction result = transactionService.getTransactionById(transactionId);

        assertNotNull(result);
        assertEquals(transactionId, result.getId());
        verify(transactionRepository).findById(transactionId);
    }

    @Test
    void testGetTransactionById_NotFound() {
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                transactionService.getTransactionById(transactionId));

        assertTrue(exception.getMessage().contains("Transaction not found"));
        verify(transactionRepository).findById(transactionId);
    }

    @Test
    void testGetAllTransactions() {
        List<Transaction> entities = Collections.singletonList(domainTransaction);
        when(transactionRepository.findAll()).thenReturn(entities);

        List<Transaction> result = transactionService.getAllTransactions();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(transactionRepository).findAll();
    }

    @Test
    void testGetTransactionsByPaymentId_Success() {
        List<Transaction> entities = Collections.singletonList(domainTransaction);
        when(transactionRepository.findByPaymentIdOrderByTimestampDesc(paymentId)).thenReturn(entities);

        List<Transaction> result = transactionService.getTransactionsByPaymentId(paymentId);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(transactionRepository).findByPaymentIdOrderByTimestampDesc(paymentId);
    }

    @Test
    void testGetTransactionsByType() {
        List<Transaction> entities = Collections.singletonList(domainTransaction);
        when(transactionRepository.findByType(TransactionType.PAYMENT)).thenReturn(entities);

        List<Transaction> result = transactionService.getTransactionsByType(TransactionType.PAYMENT);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(transactionRepository).findByType(TransactionType.PAYMENT);
    }
}
