package com.example.demo.entity;

public enum PaymentStatus {
    PENDING,
    PROCESSING,  // Payment processed by merchant, waiting for confirmation
    COMPLETED,
    FAILED,
    REFUNDED,
    CANCELLED
}
