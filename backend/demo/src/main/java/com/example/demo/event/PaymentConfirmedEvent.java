package com.example.demo.event;

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
public class PaymentConfirmedEvent {
    private Long paymentId;
    private BigDecimal amount;
    private String currency;
    private LocalDateTime timestamp;
}
