package com.example.paymentservice.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Domain model for a wallet (from user service).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Wallet {

    private Long id;
    private Long userId;
    private BigDecimal balance;
    private String currency;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
