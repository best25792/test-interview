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
public class HoldCapturedEvent {
    private Long holdId;
    private Long userId;
    private Long paymentId;
    private BigDecimal amount;
    private BigDecimal oldBalance;
    private BigDecimal newBalance;
    private LocalDateTime timestamp;
}
