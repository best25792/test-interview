package com.example.paymentservice.domain.model;

import com.example.paymentservice.entity.TransactionStatus;
import com.example.paymentservice.entity.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Domain model for a transaction (business logic layer).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    private Long id;
    private Long paymentId;
    private TransactionType type;
    private BigDecimal amount;
    private TransactionStatus status;
    private String reference;
    private String externalReference;
    private LocalDateTime timestamp;
    private String description;
}
