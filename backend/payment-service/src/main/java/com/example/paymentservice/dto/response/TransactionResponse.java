package com.example.paymentservice.dto.response;

import com.example.paymentservice.entity.TransactionStatus;
import com.example.paymentservice.entity.TransactionType;
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
public class TransactionResponse {
    
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
