package com.example.paymentservice.service;

import com.example.paymentservice.entity.Transaction;
import com.example.paymentservice.entity.TransactionType;
import com.example.paymentservice.exception.PaymentNotFoundException;
import com.example.paymentservice.repository.PaymentRepository;
import com.example.paymentservice.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private TransactionService transactionService;

    private Long transactionId = 1L;
    private Long paymentId = 100L;
    private Transaction transaction;

    @BeforeEach
    void setUp() {
        transaction = Transaction.builder()
                .id(transactionId)
                .paymentId(paymentId)
                .type(TransactionType.PAYMENT)
                .amount(new BigDecimal("50.00"))
                .status(com.example.paymentservice.entity.TransactionStatus.SUCCESS)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Test
    void testGetTransactionById_Success() {
        // Given
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));

        // When
        Transaction result = transactionService.getTransactionById(transactionId);

        // Then
        assertNotNull(result);
        assertEquals(transactionId, result.getId());
        verify(transactionRepository).findById(transactionId);
    }

    @Test
    void testGetTransactionById_NotFound() {
        // Given
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            transactionService.getTransactionById(transactionId);
        });

        assertTrue(exception.getMessage().contains("Transaction not found"));
        verify(transactionRepository).findById(transactionId);
    }

    @Test
    void testGetAllTransactions() {
        // Given
        List<Transaction> transactions = Arrays.asList(transaction);
        when(transactionRepository.findAll()).thenReturn(transactions);

        // When
        List<Transaction> result = transactionService.getAllTransactions();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(transactionRepository).findAll();
    }

    @Test
    void testGetTransactionsByPaymentId_Success() {
        // Given
        List<Transaction> transactions = Arrays.asList(transaction);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(com.example.paymentservice.entity.Payment.builder()
                .id(paymentId)
                .build()));
        when(transactionRepository.findByPaymentIdOrderByTimestampDesc(paymentId)).thenReturn(transactions);

        // When
        List<Transaction> result = transactionService.getTransactionsByPaymentId(paymentId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(paymentRepository).findById(paymentId);
        verify(transactionRepository).findByPaymentIdOrderByTimestampDesc(paymentId);
    }

    @Test
    void testGetTransactionsByPaymentId_PaymentNotFound() {
        // Given
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(PaymentNotFoundException.class, () -> {
            transactionService.getTransactionsByPaymentId(paymentId);
        });

        verify(paymentRepository).findById(paymentId);
        verify(transactionRepository, never()).findByPaymentIdOrderByTimestampDesc(anyLong());
    }

    @Test
    void testGetTransactionsByType() {
        // Given
        List<Transaction> transactions = Arrays.asList(transaction);
        when(transactionRepository.findByType(TransactionType.PAYMENT)).thenReturn(transactions);

        // When
        List<Transaction> result = transactionService.getTransactionsByType(TransactionType.PAYMENT);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(transactionRepository).findByType(TransactionType.PAYMENT);
    }
}
