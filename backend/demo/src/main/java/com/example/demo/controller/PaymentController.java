package com.example.demo.controller;

import com.example.demo.dto.request.CreatePaymentRequest;
import com.example.demo.dto.request.InitiatePaymentRequest;
import com.example.demo.dto.request.ProcessPaymentRequest;
import com.example.demo.dto.request.RefundPaymentRequest;
import com.example.demo.dto.response.InitiatePaymentResponse;
import com.example.demo.dto.response.PaymentResponse;
import com.example.demo.dto.response.QRCodeResponse;
import com.example.demo.entity.Payment;
import com.example.demo.entity.PaymentStatus;
import com.example.demo.entity.QRCode;
import com.example.demo.repository.qr.QRCodeRepository;
import com.example.demo.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final QRCodeRepository qrCodeRepository;

    /**
     * Step 1 & 2: User initiates payment - returns transaction_id and QR code
     * Creates a hold on user's wallet
     */
    @PostMapping("/initiate")
    public ResponseEntity<InitiatePaymentResponse> initiatePayment(@Valid @RequestBody InitiatePaymentRequest request) {
        InitiatePaymentResponse response = paymentService.initiatePayment(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        Payment payment = paymentService.createPayment(request);
        PaymentResponse response = mapToPaymentResponse(payment);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable Long id) {
        Payment payment = paymentService.getPaymentById(id);
        PaymentResponse response = mapToPaymentResponse(payment);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<PaymentResponse>> getAllPayments(
            @RequestParam(required = false) PaymentStatus status) {
        List<Payment> payments;
        if (status != null) {
            payments = paymentService.getPaymentsByStatus(status);
        } else {
            payments = paymentService.getAllPayments();
        }
        
        List<PaymentResponse> responses = payments.stream()
                .map(this::mapToPaymentResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }

    /**
     * Step 4: Merchant processes payment via QR scan
     */
    @PostMapping("/{id}/process")
    public ResponseEntity<PaymentResponse> processPayment(
            @PathVariable Long id,
            @Valid @RequestBody ProcessPaymentRequest request) {
        Payment payment = paymentService.processPayment(id, request);
        PaymentResponse response = mapToPaymentResponse(payment);
        return ResponseEntity.ok(response);
    }

    /**
     * Step 5: Merchant confirms payment - triggers hold capture and wallet deduction
     */
    @PostMapping("/{id}/confirm")
    public ResponseEntity<PaymentResponse> confirmPayment(@PathVariable Long id) {
        Payment payment = paymentService.confirmPayment(id);
        PaymentResponse response = mapToPaymentResponse(payment);
        return ResponseEntity.ok(response);
    }

    /**
     * Cancel payment - releases the hold
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<PaymentResponse> cancelPayment(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        Payment payment = paymentService.cancelPayment(id, reason);
        PaymentResponse response = mapToPaymentResponse(payment);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<PaymentResponse> refundPayment(
            @PathVariable Long id,
            @Valid @RequestBody RefundPaymentRequest request) {
        Payment payment = paymentService.refundPayment(id, request);
        PaymentResponse response = mapToPaymentResponse(payment);
        return ResponseEntity.ok(response);
    }

    private PaymentResponse mapToPaymentResponse(Payment payment) {
        PaymentResponse.PaymentResponseBuilder builder = PaymentResponse.builder()
                .id(payment.getId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .merchantId(payment.getMerchantId())
                .customerId(payment.getCustomerId())
                .description(payment.getDescription())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt());

        // Fetch QR code if exists
        Optional<QRCode> qrCodeOpt = qrCodeRepository.findByPaymentId(payment.getId());
        if (qrCodeOpt.isPresent()) {
            builder.qrCode(mapToQRCodeResponse(qrCodeOpt.get()));
        }

        return builder.build();
    }

    private QRCodeResponse mapToQRCodeResponse(QRCode qrCode) {
        return QRCodeResponse.builder()
                .id(qrCode.getId())
                .code(qrCode.getCode())
                .paymentId(qrCode.getPaymentId())
                .status(qrCode.getStatus())
                .expiresAt(qrCode.getExpiresAt())
                .createdAt(qrCode.getCreatedAt())
                .build();
    }
}
