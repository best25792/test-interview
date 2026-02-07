package com.example.demo.service;

import com.example.demo.dto.request.CreatePaymentRequest;
import com.example.demo.dto.request.InitiatePaymentRequest;
import com.example.demo.dto.request.ProcessPaymentRequest;
import com.example.demo.dto.request.RefundPaymentRequest;
import com.example.demo.dto.response.InitiatePaymentResponse;
import com.example.demo.entity.Hold;
import com.example.demo.entity.Payment;
import com.example.demo.entity.PaymentStatus;
import com.example.demo.event.EventPublisher;
import com.example.demo.event.PaymentConfirmedEvent;
import com.example.demo.event.PaymentCreatedEvent;
import com.example.demo.event.PaymentProcessedEvent;
import com.example.demo.event.PaymentRefundedEvent;
import com.example.demo.exception.PaymentException;
import com.example.demo.exception.PaymentNotFoundException;
import com.example.demo.repository.payment.PaymentRepository;
import com.example.demo.repository.qr.QRCodeRepository;
import com.example.demo.service.RateLimitService;
import com.example.demo.config.TracingConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Payment Service - Simulated as a separate microservice
 * Communicates with other services via events only
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final QRCodeRepository qrCodeRepository;
    private final WalletService walletService;
    private final EventPublisher eventPublisher;
    private final RateLimitService rateLimitService;
    private final OutboxService outboxService;
    private final QRCodeClientService qrCodeClientService;
    private final TracingConfig tracingConfig;

    /**
     * Step 1 & 2: User initiates payment - only customer ID is required
     * Payment details (amount, merchant, etc.) will come from merchant in processPayment
     */
    @Transactional
    public InitiatePaymentResponse initiatePayment(InitiatePaymentRequest request) {
        // Step 2: Check user data for business conditions
        if (!walletService.validateUserConditions(request.getUserId())) {
            throw new PaymentException("User does not meet business conditions");
        }

        // Create payment record with minimal information (only customer ID)
        // Amount, currency, merchantId will be set when merchant processes the payment
        Payment payment = Payment.builder()
                .amount(BigDecimal.ZERO) // Will be updated in processPayment
                .currency("USD") // Default, will be updated in processPayment
                .status(PaymentStatus.PENDING)
                .merchantId(null) // Will be set in processPayment
                .customerId(request.getUserId().toString())
                .description(null) // Will be set in processPayment
                .build();

        Payment savedPayment = paymentRepository.save(payment);
        
        // Add payment_id to trace context and MDC for correlation
        tracingConfig.addPaymentIdToContext(savedPayment.getId());
        
        // Note: Rate limiting removed from QR service as it can't query by customerId across databases
        // Rate limiting should be handled in payment service or a dedicated rate limiting service
        
        // Note: We don't create a hold here because we don't know the amount yet
        // Hold will be created in processPayment when merchant provides the amount
        
        // Call QR code service directly via RestClient to create QR code
        com.example.demo.dto.response.QRCodeResponse qrCodeResponse;
        try {
            qrCodeResponse = qrCodeClientService.createQRCode(
                    savedPayment.getId(), 
                    savedPayment.getCustomerId()
            );
        } catch (Exception e) {
            log.error("Failed to create QR code via RestClient for paymentId: {}", 
                    savedPayment.getId(), e);
            throw new PaymentException("Failed to create QR code: " + e.getMessage());
        }
        
        // Validate QR code expiration
        if (qrCodeResponse.getExpiresAt() != null && qrCodeResponse.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new PaymentException("Generated QR code has already expired. Please try again.");
        }
        
        // Get the actual QR code string from the response (code field from qr_codes table)
        String qrCodeString = qrCodeResponse.getCode();
        LocalDateTime expiresAt = qrCodeResponse.getExpiresAt();
        
        log.info("Payment initiated. Payment id: {}, Transaction id: {}, User id: {} - QR code generated: {}. " +
                "Amount and merchant details will be provided in processPayment", 
                savedPayment.getId(), savedPayment.getId(), request.getUserId(), qrCodeString);
        
        return InitiatePaymentResponse.builder()
                .transactionId(savedPayment.getId())
                .qrCode(qrCodeString) // Using the actual 'code' field from qr_codes table
                .message("Payment initiated successfully. QR code generated. Please show QR to merchant.")
                .success(true)
                .expiresAt(expiresAt) // Using the actual 'expires_at' field from qr_codes table
                .build();
    }

    @Transactional
    public Payment createPayment(CreatePaymentRequest request) {
        Payment payment = Payment.builder()
                .amount(request.getAmount())
                .currency(request.getCurrency().toUpperCase())
                .status(PaymentStatus.PENDING)
                .merchantId(request.getMerchantId())
                .customerId(request.getCustomerId())
                .description(request.getDescription())
                .build();

        Payment savedPayment = paymentRepository.save(payment);
        
        // Add payment_id to trace context and MDC for correlation
        tracingConfig.addPaymentIdToContext(savedPayment.getId());
        
        // Publish event instead of calling QRCodeService directly
        PaymentCreatedEvent event = PaymentCreatedEvent.builder()
                .paymentId(savedPayment.getId())
                .amount(savedPayment.getAmount())
                .currency(savedPayment.getCurrency())
                .merchantId(savedPayment.getMerchantId())
                .customerId(savedPayment.getCustomerId())
                .description(savedPayment.getDescription())
                .timestamp(LocalDateTime.now())
                .build();
        
        outboxService.saveEvent("PaymentCreatedEvent", event);
        
        log.info("Payment created with id: {} - PaymentCreatedEvent saved to outbox", savedPayment.getId());
        return savedPayment;
    }

    public Payment getPaymentById(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    public List<Payment> getPaymentsByStatus(PaymentStatus status) {
        return paymentRepository.findByStatus(status);
    }

    /**
     * Step 4: Merchant processes payment via QR scan
     * Merchant provides payment details: amount, currency, merchantId, description
     */
    @Transactional
    public Payment processPayment(Long paymentId, ProcessPaymentRequest request) {
        // Add payment_id to trace context and MDC for correlation
        tracingConfig.addPaymentIdToContext(paymentId);
        
        Payment payment = getPaymentById(paymentId);
        
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new PaymentException("Payment cannot be processed. Current status: " + payment.getStatus());
        }

        // Validate QR code via RestClient
        com.example.demo.dto.response.QRCodeResponse qrCodeResponse;
        try {
            qrCodeResponse = qrCodeClientService.validateQRCode(request.getQrCode());
        } catch (Exception e) {
            log.error("Failed to validate QR code via RestClient: qrCode={}", request.getQrCode(), e);
            throw new PaymentException("QR code validation failed: " + e.getMessage());
        }
        
        if (!qrCodeResponse.getPaymentId().equals(paymentId)) {
            throw new PaymentException("QR code does not match the payment");
        }
        
        if (qrCodeResponse.getStatus() != com.example.demo.entity.QRCodeStatus.ACTIVE) {
            throw new PaymentException("QR code is not active. Status: " + qrCodeResponse.getStatus());
        }
        
        // Get QR code entity to update status
        var qrCodeOpt = qrCodeRepository.findByCode(request.getQrCode());
        if (qrCodeOpt.isEmpty()) {
            throw new PaymentException("QR code not found in database: " + request.getQrCode());
        }
        var qrCode = qrCodeOpt.get();

        // Update payment with merchant-provided details
        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency().toUpperCase());
        payment.setMerchantId(request.getMerchantId());
        payment.setDescription(request.getDescription());

        // Check user has sufficient available balance for the amount
        Long userId = Long.parseLong(payment.getCustomerId());
        if (!walletService.hasSufficientBalance(userId, request.getAmount())) {
            throw new PaymentException("Insufficient available wallet balance");
        }

        // Create hold (authorization) on user's wallet with the actual amount
        Hold hold = walletService.createHold(
                userId,
                paymentId,
                request.getAmount(),
                "Payment authorization for merchant: " + request.getMerchantId()
        );

        // Mark QR code as used
        qrCode.setStatus(com.example.demo.entity.QRCodeStatus.USED);
        qrCodeRepository.save(qrCode);

        // Update payment status to PROCESSING (waiting for merchant confirmation)
        payment.setStatus(PaymentStatus.PROCESSING);
        Payment updatedPayment = paymentRepository.save(payment);

        // Save event to outbox (within same transaction)
        PaymentProcessedEvent event = PaymentProcessedEvent.builder()
                .paymentId(paymentId)
                .qrCode(request.getQrCode())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .timestamp(LocalDateTime.now())
                .build();
        
        outboxService.saveEvent("PaymentProcessedEvent", event);
        
        log.info("Payment processed by merchant. Payment id: {}, Amount: {}, Merchant: {} - " +
                "Hold created, waiting for confirmation", 
                paymentId, request.getAmount(), request.getMerchantId());
        return updatedPayment;
    }

    /**
     * Step 5 & 6: Merchant confirms payment - capture hold and deduct from user wallet
     */
    @Transactional
    public Payment confirmPayment(Long paymentId) {
        // Add payment_id to trace context and MDC for correlation
        tracingConfig.addPaymentIdToContext(paymentId);
        
        Payment payment = getPaymentById(paymentId);
        
        if (payment.getStatus() != PaymentStatus.PROCESSING) {
            throw new PaymentException("Payment cannot be confirmed. Current status: " + payment.getStatus());
        }

        // Get the hold for this payment
        Hold hold = walletService.getHoldByPaymentId(paymentId);
        
        // Step 6: Capture hold (deduct from user wallet)
        walletService.captureHold(hold.getId(), "Payment confirmed to merchant: " + payment.getMerchantId());

        // Update payment status to completed
        payment.setStatus(PaymentStatus.COMPLETED);
        Payment updatedPayment = paymentRepository.save(payment);

        // Publish event for transaction creation
        PaymentConfirmedEvent event = PaymentConfirmedEvent.builder()
                .paymentId(updatedPayment.getId())
                .amount(updatedPayment.getAmount())
                .currency(updatedPayment.getCurrency())
                .timestamp(LocalDateTime.now())
                .build();
        
        eventPublisher.publishPaymentConfirmed(event);

        log.info("Payment confirmed. Payment id: {} - Hold captured, wallet deducted - PaymentConfirmedEvent published", paymentId);
        return updatedPayment;
    }

    /**
     * Cancel payment - release the hold
     */
    @Transactional
    public Payment cancelPayment(Long paymentId, String reason) {
        // Add payment_id to trace context and MDC for correlation
        tracingConfig.addPaymentIdToContext(paymentId);
        
        Payment payment = getPaymentById(paymentId);
        
        if (payment.getStatus() != PaymentStatus.PENDING && payment.getStatus() != PaymentStatus.PROCESSING) {
            throw new PaymentException("Payment cannot be cancelled. Current status: " + payment.getStatus());
        }

        // Release the hold if it exists (only if payment was processed)
        if (payment.getStatus() == PaymentStatus.PROCESSING) {
            try {
                Hold hold = walletService.getHoldByPaymentId(paymentId);
                walletService.releaseHold(hold.getId(), reason != null ? reason : "Payment cancelled");
            } catch (PaymentException e) {
                log.warn("No hold found for paymentId: {}", paymentId);
            }
        }

        payment.setStatus(PaymentStatus.CANCELLED);
        Payment updatedPayment = paymentRepository.save(payment);

        log.info("Payment cancelled. Payment id: {} - Hold released if existed", paymentId);
        return updatedPayment;
    }

    @Transactional
    public Payment refundPayment(Long paymentId, RefundPaymentRequest request) {
        // Add payment_id to trace context and MDC for correlation
        tracingConfig.addPaymentIdToContext(paymentId);
        
        Payment payment = getPaymentById(paymentId);
        
        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new PaymentException("Only completed payments can be refunded. Current status: " + payment.getStatus());
        }

        if (request.getAmount().compareTo(payment.getAmount()) > 0) {
            throw new PaymentException("Refund amount cannot exceed payment amount");
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        Payment updatedPayment = paymentRepository.save(payment);

        // Refund to user wallet
        Long userId = Long.parseLong(payment.getCustomerId());
        walletService.addToWallet(userId, request.getAmount(), 
                "Refund for payment: " + paymentId);

        // Save event to outbox (within same transaction)
        PaymentRefundedEvent event = PaymentRefundedEvent.builder()
                .paymentId(updatedPayment.getId())
                .refundAmount(request.getAmount())
                .reason(request.getReason())
                .timestamp(LocalDateTime.now())
                .build();
        
        outboxService.saveEvent("PaymentRefundedEvent", event);

        log.info("Payment refunded. Payment id: {}, Refund amount: {} - Wallet credited - PaymentRefundedEvent published", 
                paymentId, request.getAmount());
        return updatedPayment;
    }

    // Method to update payment status (called by event listeners if needed)
    @Transactional
    public void updatePaymentStatus(Long paymentId, PaymentStatus status) {
        Payment payment = getPaymentById(paymentId);
        payment.setStatus(status);
        paymentRepository.save(payment);
        log.info("Payment status updated. Payment id: {}, New status: {}", paymentId, status);
    }
}
