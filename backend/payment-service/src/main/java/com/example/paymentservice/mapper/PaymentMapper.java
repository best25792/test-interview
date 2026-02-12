package com.example.paymentservice.mapper;

import com.example.paymentservice.domain.model.Payment;
import com.example.paymentservice.domain.model.QRCode;
import com.example.paymentservice.dto.response.PaymentResponse;
import com.example.paymentservice.dto.response.QRCodeResponse;
import com.example.paymentservice.entity.PaymentEntity;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    private final QRCodeMapper qrCodeMapper;

    public PaymentMapper(QRCodeMapper qrCodeMapper) {
        this.qrCodeMapper = qrCodeMapper;
    }

    /** Map JPA entity to domain using builder. */
    public Payment toDomain(PaymentEntity entity) {
        if (entity == null) return null;
        return Payment.builder()
                .id(entity.getId())
                .amount(entity.getAmount())
                .currency(entity.getCurrency())
                .status(entity.getStatus())
                .merchantId(entity.getMerchantId())
                .customerId(entity.getCustomerId())
                .description(entity.getDescription())
                .idempotencyKey(entity.getIdempotencyKey())
                .errorCode(entity.getErrorCode())
                .errorMessage(entity.getErrorMessage())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /** Map domain to JPA entity (for save/update). */
    public PaymentEntity toEntity(Payment domain) {
        if (domain == null) return null;
        return PaymentEntity.builder()
                .id(domain.getId())
                .amount(domain.getAmount())
                .currency(domain.getCurrency())
                .status(domain.getStatus())
                .merchantId(domain.getMerchantId())
                .customerId(domain.getCustomerId())
                .description(domain.getDescription())
                .idempotencyKey(domain.getIdempotencyKey())
                .errorCode(domain.getErrorCode())
                .errorMessage(domain.getErrorMessage())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    /** Map domain payment and optional QR code to API response. */
    public PaymentResponse toResponse(Payment payment, QRCode qrCode) {
        if (payment == null) return null;
        PaymentResponse.PaymentResponseBuilder builder = PaymentResponse.builder()
                .id(payment.getId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .merchantId(payment.getMerchantId())
                .customerId(payment.getCustomerId())
                .description(payment.getDescription())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .errorCode(payment.getErrorCode())
                .errorMessage(payment.getErrorMessage());
        if (qrCode != null) {
            QRCodeResponse qrResponse = qrCodeMapper.toResponse(qrCode);
            builder.qrCode(qrResponse);
        }
        return builder.build();
    }
}
