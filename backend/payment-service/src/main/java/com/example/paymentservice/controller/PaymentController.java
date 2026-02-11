package com.example.paymentservice.controller;

import com.example.paymentservice.client.QRCodeClientService;
import com.example.paymentservice.domain.model.Payment;
import com.example.paymentservice.domain.model.QRCode;
import com.example.paymentservice.dto.request.CreatePaymentRequest;
import com.example.paymentservice.dto.request.InitiatePaymentRequest;
import com.example.paymentservice.dto.request.ProcessPaymentRequest;
import com.example.paymentservice.dto.request.RefundPaymentRequest;
import com.example.paymentservice.dto.response.InitiatePaymentResponse;
import com.example.paymentservice.dto.response.PaymentResponse;
import com.example.paymentservice.entity.PaymentStatus;
import com.example.paymentservice.exception.ErrorResponse;
import com.example.paymentservice.mapper.PaymentMapper;
import com.example.paymentservice.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment lifecycle management - initiate, process, cancel, refund")
public class PaymentController {

    private final PaymentService paymentService;
    private final QRCodeClientService qrCodeClientService;
    private final PaymentMapper paymentMapper;

    @Operation(
            summary = "Initiate a payment",
            description = "Customer initiates a payment. Returns a transaction ID. " +
                    "QR code will be generated asynchronously - poll GET /{id}/status to get it."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Payment initiated successfully"),
            @ApiResponse(responseCode = "400", description = "User validation failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Duplicate request (same idempotency key)")
    })
    @PostMapping("/initiate")
    public ResponseEntity<InitiatePaymentResponse> initiatePayment(
            @Parameter(description = "Idempotency key to prevent duplicate payments")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody InitiatePaymentRequest request) {
        InitiatePaymentResponse response = paymentService.initiatePayment(request, idempotencyKey);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Create a payment directly", description = "Creates a payment with full details (amount, merchant, etc.)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Payment created"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        Payment payment = paymentService.createPayment(request);
        PaymentResponse response = mapToPaymentResponse(payment);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Get payment by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment found"),
            @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable Long id) {
        Payment payment = paymentService.getPaymentById(id);
        PaymentResponse response = mapToPaymentResponse(payment);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Poll payment status",
            description = "Used by frontend to check if QR code is ready or to get the final payment result"
    )
    @GetMapping("/{id}/status")
    public ResponseEntity<PaymentResponse> getPaymentStatus(@PathVariable Long id) {
        Payment payment = paymentService.getPaymentById(id);
        PaymentResponse response = mapToPaymentResponse(payment);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "List payments", description = "List all payments, optionally filtered by status")
    @GetMapping
    public ResponseEntity<List<PaymentResponse>> getAllPayments(
            @Parameter(description = "Filter by payment status")
            @RequestParam(required = false) PaymentStatus status) {
        List<Payment> payments = status != null
                ? paymentService.getPaymentsByStatus(status)
                : paymentService.getAllPayments();
        List<PaymentResponse> responses = payments.stream()
                .map(this::mapToPaymentResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @Operation(
            summary = "Process payment via QR scan",
            description = "Merchant scans customer QR code. Validates QR, deducts wallet atomically, " +
                    "and completes the payment. Returns structured error codes on failure."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment processed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid state / QR mismatch / insufficient balance",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    @PostMapping("/{id}/process")
    public ResponseEntity<PaymentResponse> processPayment(
            @PathVariable Long id,
            @Parameter(description = "Idempotency key to prevent duplicate charges")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody ProcessPaymentRequest request) {
        Payment payment = paymentService.processPayment(id, request, idempotencyKey);
        PaymentResponse response = mapToPaymentResponse(payment);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Cancel payment",
            description = "Cancel a PENDING or READY payment. Cannot cancel after processing."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment cancelled"),
            @ApiResponse(responseCode = "400", description = "Payment cannot be cancelled in current state",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{id}/cancel")
    public ResponseEntity<PaymentResponse> cancelPayment(
            @PathVariable Long id,
            @Parameter(description = "Reason for cancellation")
            @RequestParam(required = false) String reason) {
        Payment payment = paymentService.cancelPayment(id, reason);
        PaymentResponse response = mapToPaymentResponse(payment);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Refund a payment",
            description = "Refund a COMPLETED payment (full or partial). Credits the customer's wallet."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment refunded"),
            @ApiResponse(responseCode = "400", description = "Refund not allowed / amount exceeds original",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{id}/refund")
    public ResponseEntity<PaymentResponse> refundPayment(
            @PathVariable Long id,
            @Parameter(description = "Idempotency key to prevent duplicate refunds")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody RefundPaymentRequest request) {
        Payment payment = paymentService.refundPayment(id, request, idempotencyKey);
        PaymentResponse response = mapToPaymentResponse(payment);
        return ResponseEntity.ok(response);
    }

    private PaymentResponse mapToPaymentResponse(Payment payment) {
        var qrCode = getQRCodeIfExists(payment.getId());
        return paymentMapper.toResponse(payment, qrCode);
    }

    private QRCode getQRCodeIfExists(Long paymentId) {
        try {
            return qrCodeClientService.getQRCode(paymentId);
        } catch (Exception e) {
            return null;
        }
    }
}
