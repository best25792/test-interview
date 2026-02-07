package com.example.demo.dto.response;

import com.example.demo.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {
    
    private Long id;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;
    private String merchantId;
    private String customerId;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private QRCodeResponse qrCode;
}
