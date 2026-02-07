package com.example.qrservice.dto.response;

import com.example.qrservice.entity.QRCodeStatus;
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
    private QRCodeStatus status;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
