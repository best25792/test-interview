package com.example.paymentservice.service;

import com.example.paymentservice.client.QRCodeClientService;
import com.example.paymentservice.client.UserClientService;
import com.example.paymentservice.client.WalletClientService;
import com.example.paymentservice.domain.model.QRCode;
import com.example.paymentservice.dto.request.InitiatePaymentRequest;
import com.example.paymentservice.dto.request.ProcessPaymentRequest;
import com.example.paymentservice.dto.request.RefundPaymentRequest;
import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.entity.PaymentErrorCode;
import com.example.paymentservice.entity.PaymentStatus;
import com.example.paymentservice.exception.PaymentException;
import com.example.paymentservice.exception.PaymentNotFoundException;
import com.example.paymentservice.mapper.PaymentMapper;
import com.example.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private QRCodeClientService qrCodeClientService;

    @Mock
    private WalletClientService walletClientService;

    @Mock
    private UserClientService userClientService;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private PaymentService paymentService;

    private Long userId = 1L;
    private Long paymentId = 100L;
    private Payment payment;
    private com.example.paymentservice.domain.model.Payment domainPayment;
    private QRCode domainQRCode;

    @BeforeEach
    void setUp() {
        payment = Payment.builder()
                .id(paymentId)
                .amount(new BigDecimal("50.00"))
                .currency("USD")
                .status(PaymentStatus.READY)
                .customerId(userId.toString())
                .build();

        domainPayment = com.example.paymentservice.domain.model.Payment.builder()
                .id(paymentId)
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .merchantId(payment.getMerchantId())
                .customerId(payment.getCustomerId())
                .description(payment.getDescription())
                .idempotencyKey(payment.getIdempotencyKey())
                .errorCode(payment.getErrorCode())
                .errorMessage(payment.getErrorMessage())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();

        domainQRCode = QRCode.builder()
                .id(1L)
                .code("PAYMENT_100_1234567890_ABCDEF123456")
                .paymentId(paymentId)
                .status("ACTIVE")
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .createdAt(LocalDateTime.now())
                .build();

        lenient().when(paymentMapper.toDomain(any(Payment.class))).thenAnswer(inv -> {
            Payment e = inv.getArgument(0);
            return com.example.paymentservice.domain.model.Payment.builder()
                    .id(e.getId()).amount(e.getAmount()).currency(e.getCurrency())
                    .status(e.getStatus()).merchantId(e.getMerchantId()).customerId(e.getCustomerId())
                    .description(e.getDescription()).idempotencyKey(e.getIdempotencyKey())
                    .errorCode(e.getErrorCode()).errorMessage(e.getErrorMessage())
                    .createdAt(e.getCreatedAt()).updatedAt(e.getUpdatedAt())
                    .build();
        });
    }

    @Test
    void testInitiatePayment_Success() {
        // Given
        InitiatePaymentRequest request = new InitiatePaymentRequest();
        request.setUserId(userId);

        Payment savedPayment = Payment.builder()
                .id(paymentId)
                .amount(BigDecimal.ZERO)
                .currency("USD")
                .status(PaymentStatus.PENDING)
                .customerId(userId.toString())
                .build();

        when(userClientService.validateUserConditions(userId)).thenReturn(true);
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

        // When
        var response = paymentService.initiatePayment(request, null);

        // Then
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(paymentId, response.getTransactionId());
        verify(userClientService).validateUserConditions(userId);
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void testInitiatePayment_UserDoesNotMeetConditions() {
        // Given
        InitiatePaymentRequest request = new InitiatePaymentRequest();
        request.setUserId(userId);

        when(userClientService.validateUserConditions(userId)).thenReturn(false);

        // When & Then
        PaymentException exception = assertThrows(PaymentException.class, () -> {
            paymentService.initiatePayment(request, null);
        });

        assertEquals("User does not meet business conditions", exception.getMessage());
        assertEquals(PaymentErrorCode.USER_VALIDATION_FAILED.name(), exception.getErrorCode());
        verify(userClientService).validateUserConditions(userId);
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void testInitiatePayment_Idempotency() {
        // Given
        InitiatePaymentRequest request = new InitiatePaymentRequest();
        request.setUserId(userId);

        String idempotencyKey = "test-key-123";
        Payment existingPayment = Payment.builder()
                .id(paymentId)
                .status(PaymentStatus.PENDING)
                .build();

        when(paymentRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.of(existingPayment));

        // When
        var response = paymentService.initiatePayment(request, idempotencyKey);

        // Then
        assertNotNull(response);
        assertEquals(paymentId, response.getTransactionId());
        verify(userClientService, never()).validateUserConditions(anyLong());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void testGetPaymentById_Success() {
        // Given
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        // When
        com.example.paymentservice.domain.model.Payment result = paymentService.getPaymentById(paymentId);

        // Then
        assertNotNull(result);
        assertEquals(paymentId, result.getId());
        verify(paymentRepository).findById(paymentId);
    }

    @Test
    void testGetPaymentById_NotFound() {
        // Given
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        // When & Then
        PaymentNotFoundException exception = assertThrows(PaymentNotFoundException.class, () -> {
            paymentService.getPaymentById(paymentId);
        });

        assertTrue(exception.getMessage().contains("Payment not found"));
        verify(paymentRepository).findById(paymentId);
    }

    @Test
    void testProcessPayment_Success() {
        // Given
        ProcessPaymentRequest request = new ProcessPaymentRequest();
        request.setQrCode("PAYMENT_100_1234567890_ABCDEF123456");
        request.setAmount(new BigDecimal("50.00"));
        request.setCurrency("USD");
        request.setMerchantId("MERCHANT_001");
        request.setDescription("Test payment");

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(qrCodeClientService.validateQRCode(request.getQrCode())).thenReturn(domainQRCode);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        // When
        com.example.paymentservice.domain.model.Payment result = paymentService.processPayment(paymentId, request, null);

        // Then
        assertNotNull(result);
        assertEquals(PaymentStatus.COMPLETED, result.getStatus());
        verify(walletClientService).deductFromWallet(userId, request.getAmount());
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void testProcessPayment_InsufficientBalance() {
        // Given
        ProcessPaymentRequest request = new ProcessPaymentRequest();
        request.setQrCode("PAYMENT_100_1234567890_ABCDEF123456");
        request.setAmount(new BigDecimal("50.00"));
        request.setCurrency("USD");
        request.setMerchantId("MERCHANT_001");

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(qrCodeClientService.validateQRCode(request.getQrCode())).thenReturn(domainQRCode);
        doThrow(new WalletClientService.InsufficientBalanceException("Insufficient balance"))
                .when(walletClientService).deductFromWallet(userId, request.getAmount());

        // When & Then
        PaymentException exception = assertThrows(PaymentException.class, () -> {
            paymentService.processPayment(paymentId, request, null);
        });

        assertEquals(PaymentErrorCode.INSUFFICIENT_BALANCE.name(), exception.getErrorCode());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void testProcessPayment_InvalidState() {
        // Given
        payment.setStatus(PaymentStatus.PENDING); // Not READY

        ProcessPaymentRequest request = new ProcessPaymentRequest();
        request.setQrCode("PAYMENT_100_1234567890_ABCDEF123456");
        request.setAmount(new BigDecimal("50.00"));
        request.setCurrency("USD");
        request.setMerchantId("MERCHANT_001");

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        // When & Then
        PaymentException exception = assertThrows(PaymentException.class, () -> {
            paymentService.processPayment(paymentId, request, null);
        });

        assertEquals(PaymentErrorCode.INVALID_PAYMENT_STATE.name(), exception.getErrorCode());
    }

    @Test
    void testRefundPayment_Success() {
        // Given
        payment.setStatus(PaymentStatus.COMPLETED);
        RefundPaymentRequest request = new RefundPaymentRequest();
        request.setAmount(new BigDecimal("30.00"));
        request.setReason("Customer request");

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        // When
        com.example.paymentservice.domain.model.Payment result = paymentService.refundPayment(paymentId, request, null);

        // Then
        assertNotNull(result);
        assertEquals(PaymentStatus.REFUNDED, result.getStatus());
        verify(walletClientService).addToWallet(eq(userId), eq(request.getAmount()), anyString());
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void testRefundPayment_OnlyCompletedPayments() {
        // Given
        payment.setStatus(PaymentStatus.PENDING);
        RefundPaymentRequest request = new RefundPaymentRequest();
        request.setAmount(new BigDecimal("30.00"));

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        // When & Then
        PaymentException exception = assertThrows(PaymentException.class, () -> {
            paymentService.refundPayment(paymentId, request, null);
        });

        assertEquals(PaymentErrorCode.INVALID_PAYMENT_STATE.name(), exception.getErrorCode());
        verify(walletClientService, never()).addToWallet(anyLong(), any(), anyString());
    }

    @Test
    void testRefundPayment_ExceedsAmount() {
        // Given
        payment.setStatus(PaymentStatus.COMPLETED);
        RefundPaymentRequest request = new RefundPaymentRequest();
        request.setAmount(new BigDecimal("100.00")); // More than payment amount

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        // When & Then
        PaymentException exception = assertThrows(PaymentException.class, () -> {
            paymentService.refundPayment(paymentId, request, null);
        });

        assertEquals(PaymentErrorCode.REFUND_EXCEEDS_AMOUNT.name(), exception.getErrorCode());
    }

    @Test
    void testCancelPayment_FromPending() {
        // Given
        payment.setStatus(PaymentStatus.PENDING);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        // When
        com.example.paymentservice.domain.model.Payment result = paymentService.cancelPayment(paymentId, "No longer needed");

        // Then
        assertNotNull(result);
        assertEquals(PaymentStatus.CANCELLED, result.getStatus());
    }

    @Test
    void testCancelPayment_FromReady() {
        // Given
        payment.setStatus(PaymentStatus.READY);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        // When
        com.example.paymentservice.domain.model.Payment result = paymentService.cancelPayment(paymentId, "Changed mind");

        // Then
        assertNotNull(result);
        assertEquals(PaymentStatus.CANCELLED, result.getStatus());
    }

    @Test
    void testCancelPayment_CompletedPaymentCannotBeCancelled() {
        // Given
        payment.setStatus(PaymentStatus.COMPLETED);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        // When & Then
        PaymentException exception = assertThrows(PaymentException.class, () -> {
            paymentService.cancelPayment(paymentId, "Too late");
        });

        assertEquals(PaymentErrorCode.INVALID_PAYMENT_STATE.name(), exception.getErrorCode());
    }
}
