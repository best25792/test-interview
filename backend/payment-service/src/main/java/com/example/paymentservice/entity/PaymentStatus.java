package com.example.paymentservice.entity;

public enum PaymentStatus {
    PENDING,     // Payment created, waiting for QR code generation
    READY,       // QR code generated, ready for processing
    COMPLETED,   // Payment completed, wallet deducted
    FAILED,      // Payment failed (insufficient balance, QR invalid, etc.)
    REFUNDED,    // Payment refunded to customer
    CANCELLED    // Payment cancelled before completion
}
