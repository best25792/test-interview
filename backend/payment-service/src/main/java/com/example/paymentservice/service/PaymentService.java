package com.example.paymentservice.service;

import com.example.paymentservice.client.QRCodeClientService;
import com.example.paymentservice.client.UserClientService;
import com.example.paymentservice.client.WalletClientService;
import com.example.paymentservice.domain.model.Payment;
import com.example.paymentservice.domain.model.QRCode;
import com.example.paymentservice.dto.request.CreatePaymentRequest;
import com.example.paymentservice.dto.request.InitiatePaymentRequest;
import com.example.paymentservice.dto.request.ProcessPaymentRequest;
import com.example.paymentservice.dto.request.RefundPaymentRequest;
import com.example.paymentservice.dto.response.InitiatePaymentResponse;
import com.example.paymentservice.entity.PaymentErrorCode;
import com.example.paymentservice.entity.PaymentStatus;
import com.example.paymentservice.exception.PaymentException;
import com.example.paymentservice.exception.PaymentNotFoundException;
import com.example.paymentservice.mapper.PaymentMapper;
import com.example.paymentservice.repository.PaymentRepository;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Payment Service - Manages payments and transactions.
 * Uses domain layer: reads/writes go through repository with entity↔domain mapping.
 *
 * Industry-standard patterns applied:
 * - Idempotency keys to prevent duplicate charges on client retry
 * - HTTP calls outside @Transactional to avoid connection pool exhaustion
 * - Structured error codes for programmatic client handling
 * - Single-call wallet deduction (no hold/capture flow)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final QRCodeClientService qrCodeClientService;
    private final WalletClientService walletClientService;
    private final UserClientService userClientService;
    private final OutboxService outboxService;

    /**
     * Step 1 & 2: User initiates payment.
     * Validates user conditions, saves payment with PENDING status,
     * and publishes event for QR code generation.
     * 
     * HTTP call to user-service is made OUTSIDE the transaction.
     */
    public InitiatePaymentResponse initiatePayment(InitiatePaymentRequest request, String idempotencyKey) {
        // 1. Idempotency check (before any processing)
        if (idempotencyKey != null) {
            Optional<com.example.paymentservice.entity.Payment> existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                Payment payment = paymentMapper.toDomain(existing.get());
                log.info("Duplicate request detected for idempotencyKey: {}, returning existing payment: {}",
                        idempotencyKey, payment.getId());
                return InitiatePaymentResponse.builder()
                        .transactionId(payment.getId())
                        .qrCode(null)
                        .message("Payment already initiated. Please poll payment status to get QR code.")
                        .success(true)
                        .expiresAt(null)
                        .build();
            }
        }

        // 2. Validate user conditions (HTTP call - OUTSIDE transaction, protected by circuit breaker)
        try {
            if (!userClientService.validateUserConditions(request.getUserId())) {
                throw new PaymentException(
                        PaymentErrorCode.USER_VALIDATION_FAILED.name(),
                        "User does not meet business conditions");
            }
        } catch (CallNotPermittedException e) {
            throw new PaymentException(
                    PaymentErrorCode.SERVICE_UNAVAILABLE.name(),
                    "User service is temporarily unavailable. Please retry later.");
        }

        // 3. Short transaction: save payment + outbox event
        Payment savedPayment = saveInitiatedPayment(request, idempotencyKey);

        log.info("Payment initiated. Payment id: {}, User id: {} - PaymentCreatedEvent published. " +
                "QR code will be generated asynchronously.", savedPayment.getId(), request.getUserId());

        return InitiatePaymentResponse.builder()
                .transactionId(savedPayment.getId())
                .qrCode(null)
                .message("Payment initiated successfully. Please poll payment status to get QR code.")
                .success(true)
                .expiresAt(null)
                .build();
    }

    @Transactional
    protected Payment saveInitiatedPayment(InitiatePaymentRequest request, String idempotencyKey) {
        com.example.paymentservice.entity.Payment entity = com.example.paymentservice.entity.Payment.builder()
                .amount(BigDecimal.ZERO)
                .currency("USD")
                .status(PaymentStatus.PENDING)
                .merchantId(null)
                .customerId(request.getUserId().toString())
                .description(null)
                .idempotencyKey(idempotencyKey)
                .build();

        com.example.paymentservice.entity.Payment saved = paymentRepository.save(entity);

        PaymentCreatedEvent event = PaymentCreatedEvent.builder()
                .paymentId(saved.getId())
                .amount(saved.getAmount())
                .currency(saved.getCurrency())
                .merchantId(saved.getMerchantId())
                .customerId(saved.getCustomerId())
                .description(saved.getDescription())
                .timestamp(LocalDateTime.now())
                .build();

        outboxService.saveEvent("PaymentCreatedEvent", event);
        return paymentMapper.toDomain(saved);
    }

    @Transactional
    public Payment createPayment(CreatePaymentRequest request) {
        com.example.paymentservice.entity.Payment entity = com.example.paymentservice.entity.Payment.builder()
                .amount(request.getAmount())
                .currency(request.getCurrency().toUpperCase())
                .status(PaymentStatus.PENDING)
                .merchantId(request.getMerchantId())
                .customerId(request.getCustomerId())
                .description(request.getDescription())
                .build();

        com.example.paymentservice.entity.Payment saved = paymentRepository.save(entity);

        PaymentCreatedEvent event = PaymentCreatedEvent.builder()
                .paymentId(saved.getId())
                .amount(saved.getAmount())
                .currency(saved.getCurrency())
                .merchantId(saved.getMerchantId())
                .customerId(saved.getCustomerId())
                .description(saved.getDescription())
                .timestamp(LocalDateTime.now())
                .build();

        outboxService.saveEvent("PaymentCreatedEvent", event);

        log.info("Payment created with id: {} - PaymentCreatedEvent saved to outbox", saved.getId());
        return paymentMapper.toDomain(saved);
    }

    public Payment getPaymentById(Long id) {
        com.example.paymentservice.entity.Payment entity = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));
        return paymentMapper.toDomain(entity);
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll().stream()
                .map(paymentMapper::toDomain)
                .toList();
    }

    public List<Payment> getPaymentsByStatus(PaymentStatus status) {
        return paymentRepository.findByStatus(status).stream()
                .map(paymentMapper::toDomain)
                .toList();
    }

    /**
     * Step 4: Merchant processes payment via QR scan.
     *
     * All HTTP calls (QR validation, wallet deduction) happen OUTSIDE the transaction.
     * Only the final DB write is transactional (short connection hold time).
     *
     * Supports idempotency: same idempotency key returns existing result.
     */
    public Payment processPayment(Long paymentId, ProcessPaymentRequest request, String idempotencyKey) {
        // 1. Idempotency check
        if (idempotencyKey != null) {
            Optional<com.example.paymentservice.entity.Payment> existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                log.info("Duplicate process request for idempotencyKey: {}, returning existing payment: {}",
                        idempotencyKey, existing.get().getId());
                return paymentMapper.toDomain(existing.get());
            }
        }

        // 2. Quick state validation (read-only, no transaction needed)
        Payment payment = getPaymentById(paymentId);
        if (payment.getStatus() != PaymentStatus.READY) {
            throw new PaymentException(
                    PaymentErrorCode.INVALID_PAYMENT_STATE.name(),
                    "Payment cannot be processed. Current status: " + payment.getStatus() +
                            ". Payment must be in READY status (QR code must be generated first).");
        }

        // 3. Validate QR code (HTTP call - OUTSIDE transaction, protected by circuit breaker)
        QRCode qrCode;
        try {
            qrCode = qrCodeClientService.validateQRCode(request.getQrCode());
        } catch (CallNotPermittedException e) {
            throw new PaymentException(
                    PaymentErrorCode.SERVICE_UNAVAILABLE.name(),
                    "QR code service is temporarily unavailable. Please retry later.");
        } catch (Exception e) {
            log.error("Failed to validate QR code via RestClient: qrCode={}", request.getQrCode(), e);
            throw new PaymentException(
                    PaymentErrorCode.QR_CODE_INVALID.name(),
                    "QR code validation failed: " + e.getMessage());
        }

        if (!qrCode.getPaymentId().equals(paymentId)) {
            throw new PaymentException(
                    PaymentErrorCode.QR_CODE_MISMATCH.name(),
                    "QR code does not match the payment");
        }

        if (!"ACTIVE".equals(qrCode.getStatus())) {
            throw new PaymentException(
                    PaymentErrorCode.QR_CODE_EXPIRED.name(),
                    "QR code is not active. Status: " + qrCode.getStatus());
        }

        // 4. Deduct from wallet (HTTP call - OUTSIDE transaction, protected by circuit breaker)
        Long userId = Long.parseLong(payment.getCustomerId());
        try {
            walletClientService.deductFromWallet(userId, request.getAmount());
        } catch (CallNotPermittedException e) {
            throw new PaymentException(
                    PaymentErrorCode.SERVICE_UNAVAILABLE.name(),
                    "Wallet service is temporarily unavailable. Please retry later.");
        } catch (WalletClientService.InsufficientBalanceException e) {
            throw new PaymentException(
                    PaymentErrorCode.INSUFFICIENT_BALANCE.name(),
                    "Insufficient available wallet balance");
        } catch (WalletClientService.WalletServiceException e) {
            throw new PaymentException(
                    PaymentErrorCode.WALLET_SERVICE_ERROR.name(),
                    "Wallet service error: " + e.getMessage());
        }

        // 5. Short transaction: update payment status + save outbox event
        return saveCompletedPayment(paymentId, request, idempotencyKey);
    }

    @Transactional
    protected Payment saveCompletedPayment(Long paymentId, ProcessPaymentRequest request, String idempotencyKey) {
        com.example.paymentservice.entity.Payment entity = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        entity.setAmount(request.getAmount());
        entity.setCurrency(request.getCurrency().toUpperCase());
        entity.setMerchantId(request.getMerchantId());
        entity.setDescription(request.getDescription());
        entity.setStatus(PaymentStatus.COMPLETED);
        entity.setIdempotencyKey(idempotencyKey);

        com.example.paymentservice.entity.Payment updated = paymentRepository.save(entity);

        PaymentProcessedEvent event = PaymentProcessedEvent.builder()
                .paymentId(paymentId)
                .qrCode(request.getQrCode())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .timestamp(LocalDateTime.now())
                .build();

        outboxService.saveEvent("PaymentProcessedEvent", event);

        log.info("Payment completed. Payment id: {}, Amount: {}, Merchant: {} - Wallet deducted",
                paymentId, request.getAmount(), request.getMerchantId());
        return paymentMapper.toDomain(updated);
    }

    /**
     * Cancel payment.
     * Only PENDING and READY payments can be cancelled (no wallet deduction to reverse).
     */
    @Transactional
    public Payment cancelPayment(Long paymentId, String reason) {
        Payment payment = getPaymentById(paymentId);

        if (payment.getStatus() != PaymentStatus.PENDING && payment.getStatus() != PaymentStatus.READY) {
            throw new PaymentException(
                    PaymentErrorCode.INVALID_PAYMENT_STATE.name(),
                    "Payment cannot be cancelled. Current status: " + payment.getStatus() +
                            ". Only PENDING or READY payments can be cancelled.");
        }

        com.example.paymentservice.entity.Payment entity = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        entity.setStatus(PaymentStatus.CANCELLED);
        com.example.paymentservice.entity.Payment updated = paymentRepository.save(entity);

        log.info("Payment cancelled. Payment id: {}, Reason: {}", paymentId, reason);
        return paymentMapper.toDomain(updated);
    }

    /**
     * Refund a completed payment.
     *
     * HTTP call to wallet service happens OUTSIDE the transaction.
     * Supports idempotency via idempotency key.
     */
    public Payment refundPayment(Long paymentId, RefundPaymentRequest request, String idempotencyKey) {
        // 1. Idempotency check
        if (idempotencyKey != null) {
            Optional<com.example.paymentservice.entity.Payment> existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent() && existing.get().getStatus() == PaymentStatus.REFUNDED) {
                log.info("Duplicate refund request for idempotencyKey: {}, returning existing payment: {}",
                        idempotencyKey, existing.get().getId());
                return paymentMapper.toDomain(existing.get());
            }
        }

        Payment payment = getPaymentById(paymentId);

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new PaymentException(
                    PaymentErrorCode.INVALID_PAYMENT_STATE.name(),
                    "Only completed payments can be refunded. Current status: " + payment.getStatus());
        }

        if (request.getAmount().compareTo(payment.getAmount()) > 0) {
            throw new PaymentException(
                    PaymentErrorCode.REFUND_EXCEEDS_AMOUNT.name(),
                    "Refund amount cannot exceed payment amount");
        }

        // 2. Refund to wallet (HTTP call - OUTSIDE transaction, protected by circuit breaker)
        Long userId = Long.parseLong(payment.getCustomerId());
        try {
            walletClientService.addToWallet(userId, request.getAmount(),
                    "Refund for payment: " + paymentId);
        } catch (CallNotPermittedException e) {
            throw new PaymentException(
                    PaymentErrorCode.SERVICE_UNAVAILABLE.name(),
                    "Wallet service is temporarily unavailable. Please retry later.");
        } catch (WalletClientService.WalletServiceException e) {
            throw new PaymentException(
                    PaymentErrorCode.WALLET_SERVICE_ERROR.name(),
                    "Failed to refund to wallet: " + e.getMessage());
        }

        // 3. Short transaction: update payment + outbox event
        return saveRefundedPayment(paymentId, request, idempotencyKey);
    }

    @Transactional
    protected Payment saveRefundedPayment(Long paymentId, RefundPaymentRequest request, String idempotencyKey) {
        com.example.paymentservice.entity.Payment entity = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        entity.setStatus(PaymentStatus.REFUNDED);
        if (idempotencyKey != null) {
            entity.setIdempotencyKey(idempotencyKey);
        }
        com.example.paymentservice.entity.Payment updated = paymentRepository.save(entity);

        PaymentRefundedEvent event = PaymentRefundedEvent.builder()
                .paymentId(updated.getId())
                .refundAmount(request.getAmount())
                .reason(request.getReason())
                .timestamp(LocalDateTime.now())
                .build();

        outboxService.saveEvent("PaymentRefundedEvent", event);

        log.info("Payment refunded. Payment id: {}, Refund amount: {} - Wallet credited",
                paymentId, request.getAmount());
        return paymentMapper.toDomain(updated);
    }

    // Event DTOs for outbox
    @lombok.Builder
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PaymentCreatedEvent {
        private Long paymentId;
        private BigDecimal amount;
        private String currency;
        private String merchantId;
        private String customerId;
        private String description;
        private LocalDateTime timestamp;
    }
    
    @lombok.Builder
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PaymentProcessedEvent {
        private Long paymentId;
        private String qrCode;
        private BigDecimal amount;
        private String currency;
        private LocalDateTime timestamp;
    }
    
    @lombok.Builder
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PaymentRefundedEvent {
        private Long paymentId;
        private BigDecimal refundAmount;
        private String reason;
        private LocalDateTime timestamp;
    }
}
