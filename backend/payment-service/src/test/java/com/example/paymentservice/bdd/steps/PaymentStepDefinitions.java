package com.example.paymentservice.bdd.steps;

import com.example.paymentservice.client.QRCodeClientService;
import com.example.paymentservice.client.UserClientService;
import com.example.paymentservice.client.WalletClientService;
import com.example.paymentservice.dto.request.InitiatePaymentRequest;
import com.example.paymentservice.dto.request.ProcessPaymentRequest;
import com.example.paymentservice.dto.request.RefundPaymentRequest;
import com.example.paymentservice.dto.response.InitiatePaymentResponse;
import com.example.paymentservice.dto.response.QRCodeResponse;
import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.entity.PaymentStatus;
import com.example.paymentservice.exception.PaymentException;
import com.example.paymentservice.repository.PaymentRepository;
import com.example.paymentservice.service.PaymentService;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class PaymentStepDefinitions {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UserClientService userClientService;

    @Autowired
    private QRCodeClientService qrCodeClientService;

    @Autowired
    private WalletClientService walletClientService;

    // Shared state between steps
    private Long userId;
    private Payment currentPayment;
    private final List<InitiatePaymentResponse> initiateResponses = new ArrayList<>();
    private PaymentException caughtException;
    private String currentQrCode;

    @Before
    public void setUp() {
        reset(userClientService, qrCodeClientService, walletClientService);
        initiateResponses.clear();
        caughtException = null;
        currentPayment = null;
        currentQrCode = null;
    }

    // ========== GIVEN ==========

    @Given("the payment system is running")
    public void thePaymentSystemIsRunning() {
        // Spring context is already started
    }

    @Given("a customer with user ID {long} exists")
    public void customerExists(Long userId) {
        this.userId = userId;
    }

    @Given("the customer meets business conditions")
    public void customerMeetsConditions() {
        when(userClientService.validateUserConditions(userId)).thenReturn(true);
    }

    @Given("the customer does NOT meet business conditions")
    public void customerDoesNotMeetConditions() {
        when(userClientService.validateUserConditions(userId)).thenReturn(false);
    }

    @Given("the customer has wallet balance of {bigdecimal}")
    public void customerHasBalance(BigDecimal balance) {
        // Wallet deduction will succeed (balance is sufficient)
        doNothing().when(walletClientService).deductFromWallet(eq(userId), any(BigDecimal.class));
    }

    @Given("the customer has insufficient wallet balance for {bigdecimal}")
    public void customerHasInsufficientBalance(BigDecimal amount) {
        doThrow(new WalletClientService.InsufficientBalanceException("Insufficient balance"))
                .when(walletClientService).deductFromWallet(eq(userId), any(BigDecimal.class));
    }

    @Given("the customer has a payment in {string} status with QR code {string}")
    public void customerHasPaymentWithQR(String status, String qrCode) {
        currentPayment = Payment.builder()
                .amount(BigDecimal.ZERO)
                .currency("USD")
                .status(PaymentStatus.valueOf(status))
                .customerId(userId.toString())
                .build();
        currentPayment = paymentRepository.save(currentPayment);
        this.currentQrCode = qrCode;

        // Mock QR validation - active and matching
        when(qrCodeClientService.validateQRCode(qrCode)).thenReturn(
                QRCodeResponse.builder()
                        .id(1L)
                        .code(qrCode)
                        .paymentId(currentPayment.getId())
                        .status("ACTIVE")
                        .expiresAt(LocalDateTime.now().plusMinutes(15))
                        .build()
        );
    }

    @Given("the customer has a payment in {string} status")
    public void customerHasPayment(String status) {
        currentPayment = Payment.builder()
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .status(PaymentStatus.valueOf(status))
                .customerId(userId != null ? userId.toString() : "1")
                .build();
        currentPayment = paymentRepository.save(currentPayment);
    }

    @Given("the QR code is expired")
    public void qrCodeExpired() {
        this.currentQrCode = "EXPIRED_QR";
        when(qrCodeClientService.validateQRCode(currentQrCode)).thenReturn(
                QRCodeResponse.builder()
                        .id(1L)
                        .code(currentQrCode)
                        .paymentId(currentPayment.getId())
                        .status("EXPIRED")
                        .build()
        );
    }

    @Given("the QR code belongs to a different payment")
    public void qrCodeBelongsToDifferentPayment() {
        this.currentQrCode = "MISMATCHED_QR";
        when(qrCodeClientService.validateQRCode(currentQrCode)).thenReturn(
                QRCodeResponse.builder()
                        .id(1L)
                        .code(currentQrCode)
                        .paymentId(99999L)  // Different payment ID
                        .status("ACTIVE")
                        .build()
        );
    }

    @Given("a completed payment of {bigdecimal} for customer with user ID {long}")
    public void completedPayment(BigDecimal amount, Long userId) {
        this.userId = userId;
        currentPayment = Payment.builder()
                .amount(amount)
                .currency("USD")
                .status(PaymentStatus.COMPLETED)
                .customerId(userId.toString())
                .build();
        currentPayment = paymentRepository.save(currentPayment);
    }

    // ========== WHEN ==========

    @When("the customer initiates a payment")
    public void customerInitiatesPayment() {
        initiatePaymentWithKey(null);
    }

    @When("the customer initiates a payment with idempotency key {string}")
    public void customerInitiatesPaymentWithKey(String key) {
        initiatePaymentWithKey(key);
    }

    @When("the customer initiates a payment again with idempotency key {string}")
    public void customerInitiatesPaymentAgainWithKey(String key) {
        initiatePaymentWithKey(key);
    }

    private void initiatePaymentWithKey(String key) {
        InitiatePaymentRequest request = new InitiatePaymentRequest();
        request.setUserId(userId);

        try {
            InitiatePaymentResponse response = paymentService.initiatePayment(request, key);
            initiateResponses.add(response);
        } catch (PaymentException e) {
            caughtException = e;
        }
    }

    @When("the merchant scans QR code {string} with amount {bigdecimal}")
    public void merchantScansQR(String qrCode, BigDecimal amount) {
        ProcessPaymentRequest request = new ProcessPaymentRequest();
        request.setQrCode(qrCode);
        request.setAmount(amount);
        request.setCurrency("USD");
        request.setMerchantId("MERCHANT_001");
        request.setDescription("Payment via QR scan");

        try {
            currentPayment = paymentService.processPayment(
                    currentPayment.getId(), request, null);
        } catch (PaymentException e) {
            caughtException = e;
        }
    }

    @When("the merchant scans the expired QR code with amount {bigdecimal}")
    public void merchantScansExpiredQR(BigDecimal amount) {
        ProcessPaymentRequest request = new ProcessPaymentRequest();
        request.setQrCode(currentQrCode);
        request.setAmount(amount);
        request.setCurrency("USD");
        request.setMerchantId("MERCHANT_001");

        try {
            currentPayment = paymentService.processPayment(
                    currentPayment.getId(), request, null);
        } catch (PaymentException e) {
            caughtException = e;
        }
    }

    @When("the merchant scans the mismatched QR code with amount {bigdecimal}")
    public void merchantScansMismatchedQR(BigDecimal amount) {
        ProcessPaymentRequest request = new ProcessPaymentRequest();
        request.setQrCode(currentQrCode);
        request.setAmount(amount);
        request.setCurrency("USD");
        request.setMerchantId("MERCHANT_001");

        try {
            currentPayment = paymentService.processPayment(
                    currentPayment.getId(), request, null);
        } catch (PaymentException e) {
            caughtException = e;
        }
    }

    @When("the merchant tries to process the payment with amount {bigdecimal}")
    public void merchantTriesToProcess(BigDecimal amount) {
        ProcessPaymentRequest request = new ProcessPaymentRequest();
        request.setQrCode("ANY_QR_CODE");
        request.setAmount(amount);
        request.setCurrency("USD");
        request.setMerchantId("MERCHANT_001");

        try {
            currentPayment = paymentService.processPayment(
                    currentPayment.getId(), request, null);
        } catch (PaymentException e) {
            caughtException = e;
        }
    }

    @When("a refund of {bigdecimal} is requested")
    public void refundRequested(BigDecimal amount) {
        RefundPaymentRequest request = new RefundPaymentRequest();
        request.setAmount(amount);
        request.setReason("Customer request");

        try {
            currentPayment = paymentService.refundPayment(
                    currentPayment.getId(), request, null);
        } catch (PaymentException e) {
            caughtException = e;
        }
    }

    // ========== THEN ==========

    @Then("the payment should be created with status {string}")
    public void paymentCreatedWithStatus(String status) {
        assertNull(caughtException, "Expected no exception but got: " +
                (caughtException != null ? caughtException.getMessage() : ""));
        assertFalse(initiateResponses.isEmpty(), "Expected at least one initiate response");

        Long paymentId = initiateResponses.get(0).getTransactionId();
        Payment payment = paymentRepository.findById(paymentId).orElseThrow();
        assertEquals(PaymentStatus.valueOf(status), payment.getStatus());
    }

    @Then("the payment status should be {string}")
    public void paymentStatusShouldBe(String status) {
        assertNull(caughtException, "Expected no exception but got: " +
                (caughtException != null ? caughtException.getMessage() : ""));
        Payment refreshed = paymentRepository.findById(currentPayment.getId()).orElseThrow();
        assertEquals(PaymentStatus.valueOf(status), refreshed.getStatus());
    }

    @Then("the payment should be rejected with error code {string}")
    public void paymentRejectedWithErrorCode(String errorCode) {
        assertNotNull(caughtException, "Expected PaymentException but none was thrown");
        assertEquals(errorCode, caughtException.getErrorCode());
    }

    @Then("the refund should be rejected with error code {string}")
    public void refundRejectedWithErrorCode(String errorCode) {
        assertNotNull(caughtException, "Expected PaymentException but none was thrown");
        assertEquals(errorCode, caughtException.getErrorCode());
    }

    @Then("no payment record should be created")
    public void noPaymentCreated() {
        assertTrue(initiateResponses.isEmpty(), "Expected no payment to be created");
    }

    @Then("{bigdecimal} should be deducted from the customer's wallet")
    public void walletDeducted(BigDecimal amount) {
        verify(walletClientService).deductFromWallet(eq(userId), eq(amount));
    }

    @And("the merchant ID should be recorded on the payment")
    public void merchantIdRecorded() {
        Payment refreshed = paymentRepository.findById(currentPayment.getId()).orElseThrow();
        assertNotNull(refreshed.getMerchantId(), "Merchant ID should be recorded");
    }

    @Then("{bigdecimal} should be credited to the customer's wallet")
    public void walletCredited(BigDecimal amount) {
        verify(walletClientService).addToWallet(eq(userId), eq(amount), anyString());
    }

    @Then("no money should be credited to the customer's wallet")
    public void noWalletCredit() {
        verify(walletClientService, never()).addToWallet(anyLong(), any(), anyString());
    }

    @Then("both responses should return the same payment ID")
    public void samePaymentId() {
        assertEquals(2, initiateResponses.size(), "Expected 2 responses");
        assertEquals(
                initiateResponses.get(0).getTransactionId(),
                initiateResponses.get(1).getTransactionId(),
                "Both responses should have the same payment ID"
        );
    }

    @Then("two separate payments should exist")
    public void twoSeparatePayments() {
        assertEquals(2, initiateResponses.size(), "Expected 2 responses");
        assertNotEquals(
                initiateResponses.get(0).getTransactionId(),
                initiateResponses.get(1).getTransactionId(),
                "Responses should have different payment IDs"
        );
    }
}
