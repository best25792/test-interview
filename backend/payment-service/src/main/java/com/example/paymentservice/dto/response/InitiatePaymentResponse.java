package com.example.paymentservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InitiatePaymentResponse {
    
    private Long transactionId;
    private String qrCode;
    private String message;
    private boolean success;
    private LocalDateTime expiresAt;
}
