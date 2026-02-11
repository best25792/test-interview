package com.example.paymentservice.domain.model;

import com.example.paymentservice.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Domain model for a payment (business logic layer).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    private Long id;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;
    private String merchantId;
    private String customerId;
    private String description;
    private String idempotencyKey;
    private String errorCode;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
