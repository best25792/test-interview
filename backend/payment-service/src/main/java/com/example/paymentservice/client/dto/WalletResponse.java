package com.example.paymentservice.client.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Wallet info returned by User Service (GET /users/{id}/wallet). */
public record WalletResponse(
        Long id,
        Long userId,
        BigDecimal balance,
        String currency,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
