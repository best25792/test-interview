package com.example.paymentservice.service;

import com.example.paymentservice.domain.model.Transaction;
import com.example.paymentservice.entity.TransactionEntity;
import com.example.paymentservice.entity.TransactionStatus;
import com.example.paymentservice.entity.TransactionType;
import com.example.paymentservice.exception.PaymentNotFoundException;
import com.example.paymentservice.mapper.TransactionMapper;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private TransactionMapper transactionMapper;

    @InjectMocks
    private TransactionService transactionService;

    private final Long transactionId = 1L;
    private final Long paymentId = 100L;
    private TransactionEntity transactionEntity;

    @BeforeEach
    void setUp() {
        transactionEntity = TransactionEntity.builder()
                .id(transactionId)
                .paymentId(paymentId)
                .type(TransactionType.PAYMENT)
                .amount(new BigDecimal("50.00"))
                .status(TransactionStatus.SUCCESS)
                .timestamp(LocalDateTime.now())
                .build();
        Transaction domainTransaction = Transaction.builder()
                .id(transactionId)
                .paymentId(paymentId)
                .type(TransactionType.PAYMENT)
                .amount(new BigDecimal("50.00"))
                .status(TransactionStatus.SUCCESS)
                .timestamp(LocalDateTime.now())
                .build();
        lenient().when(transactionMapper.toDomain(any(TransactionEntity.class))).thenReturn(domainTransaction);
    }

    @Test
    void testGetTransactionById_Success() {
        // Given
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transactionEntity));

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
        RuntimeException exception = assertThrows(RuntimeException.class, () -> transactionService.getTransactionById(transactionId));

        assertTrue(exception.getMessage().contains("Transaction not found"));
        verify(transactionRepository).findById(transactionId);
    }

    @Test
    void testGetAllTransactions() {
        // Given
        List<TransactionEntity> entities = Collections.singletonList(transactionEntity);
        when(transactionRepository.findAll()).thenReturn(entities);

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
        List<TransactionEntity> entities = Collections.singletonList(transactionEntity);
        when(paymentRepository.existsById(paymentId)).thenReturn(true);
        when(transactionRepository.findByPaymentIdOrderByTimestampDesc(paymentId)).thenReturn(entities);

        // When
        List<Transaction> result = transactionService.getTransactionsByPaymentId(paymentId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(paymentRepository).existsById(paymentId);
        verify(transactionRepository).findByPaymentIdOrderByTimestampDesc(paymentId);
    }

    @Test
    void testGetTransactionsByPaymentId_PaymentNotFound() {
        // Given
        when(paymentRepository.existsById(paymentId)).thenReturn(false);

        // When & Then
        assertThrows(PaymentNotFoundException.class, () -> transactionService.getTransactionsByPaymentId(paymentId));

        verify(paymentRepository).existsById(paymentId);
        verify(transactionRepository, never()).findByPaymentIdOrderByTimestampDesc(anyLong());
    }

    @Test
    void testGetTransactionsByType() {
        // Given
        List<TransactionEntity> entities = Collections.singletonList(transactionEntity);
        when(transactionRepository.findByType(TransactionType.PAYMENT)).thenReturn(entities);

        // When
        List<Transaction> result = transactionService.getTransactionsByType(TransactionType.PAYMENT);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(transactionRepository).findByType(TransactionType.PAYMENT);
    }
}
