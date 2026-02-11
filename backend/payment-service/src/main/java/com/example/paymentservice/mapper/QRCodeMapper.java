package com.example.paymentservice.mapper;

import com.example.paymentservice.domain.model.QRCode;
import com.example.paymentservice.dto.response.QRCodeResponse;
import org.springframework.stereotype.Component;

@Component
public class QRCodeMapper {

    /** Map HTTP/API response to domain using builder. */
    public QRCode toDomain(QRCodeResponse response) {
        if (response == null) return null;
        return QRCode.builder()
                .id(response.getId())
                .code(response.getCode())
                .paymentId(response.getPaymentId())
                .status(response.getStatus())
                .expiresAt(response.getExpiresAt())
                .createdAt(response.getCreatedAt())
                .build();
    }

    /** Map domain to API response DTO (e.g. for PaymentResponse.qrCode). */
    public QRCodeResponse toResponse(QRCode domain) {
        if (domain == null) return null;
        return QRCodeResponse.builder()
                .id(domain.getId())
                .code(domain.getCode())
                .paymentId(domain.getPaymentId())
                .status(domain.getStatus())
                .expiresAt(domain.getExpiresAt())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
