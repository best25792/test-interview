package com.example.paymentservice.entity;

/**
 * Standardized error codes for payment operations.
 * Clients can use these codes to programmatically handle specific failure scenarios.
 */
public enum PaymentErrorCode {
    INSUFFICIENT_BALANCE,
    QR_CODE_INVALID,
    QR_CODE_EXPIRED,
    QR_CODE_MISMATCH,
    INVALID_PAYMENT_STATE,
    USER_VALIDATION_FAILED,
    WALLET_SERVICE_ERROR,
    DUPLICATE_REQUEST,
    REFUND_EXCEEDS_AMOUNT,
    SERVICE_UNAVAILABLE
}
