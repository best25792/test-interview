package com.example.paymentservice.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Domain model for a QR code (from QR code service).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QRCode {

    private Long id;
    private String code;
    private Long paymentId;
    private String status;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
