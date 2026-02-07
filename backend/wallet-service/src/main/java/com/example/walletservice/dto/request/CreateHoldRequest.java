package com.example.walletservice.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateHoldRequest {
    private Long userId;
    private Long paymentId;
    private BigDecimal amount;
    private String reason;
}
