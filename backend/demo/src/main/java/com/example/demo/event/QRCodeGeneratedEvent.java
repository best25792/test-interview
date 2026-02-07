package com.example.demo.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QRCodeGeneratedEvent {
    private Long qrCodeId;
    private String qrCode;
    private Long paymentId;
    private LocalDateTime expiresAt;
    private LocalDateTime timestamp;
}
