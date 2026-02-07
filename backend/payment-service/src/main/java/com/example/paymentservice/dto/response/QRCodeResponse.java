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
public class QRCodeResponse {
    
    private Long id;
    private String code;
    private Long paymentId;
    private String status;  // Using String to avoid dependency on QR service enum
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
