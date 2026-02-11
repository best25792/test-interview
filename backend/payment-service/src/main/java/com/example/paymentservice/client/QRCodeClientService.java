package com.example.paymentservice.client;

import com.example.paymentservice.domain.model.QRCode;
import com.example.paymentservice.dto.request.CreateQRCodeRequest;
import com.example.paymentservice.dto.request.ValidateQRCodeRequest;
import com.example.paymentservice.dto.response.QRCodeResponse;
import com.example.paymentservice.exception.QRCodeNotFoundException;
import com.example.paymentservice.client.api.QRCodeApi;
import com.example.paymentservice.mapper.QRCodeMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Wrapper around QRCodeApi (HTTP Service client). Returns domain QRCode;
 * adds Resilience4j and maps 404 to QRCodeNotFoundException.
 */
@Service
@Slf4j
public class QRCodeClientService {

    private final QRCodeApi qrCodeApi;
    private final QRCodeMapper qrCodeMapper;

    public QRCodeClientService(QRCodeApi qrCodeApi, QRCodeMapper qrCodeMapper) {
        this.qrCodeApi = qrCodeApi;
        this.qrCodeMapper = qrCodeMapper;
    }

    @CircuitBreaker(name = "qrCodeService")
    @Retry(name = "qrCodeService")
    public QRCode createQRCode(Long paymentId, String customerId) {
        try {
            QRCodeResponse response = qrCodeApi.create(new CreateQRCodeRequest(paymentId, customerId));
            log.info("QR code created successfully: paymentId={}, qrCodeId={}", paymentId, response.getId());
            return qrCodeMapper.toDomain(response);
        } catch (RestClientResponseException e) {
            log.error("Error creating QR code: paymentId={}, status={}", paymentId, e.getStatusCode(), e);
            throw new RuntimeException("Failed to create QR code: " + e.getMessage(), e);
        }
    }

    @CircuitBreaker(name = "qrCodeService")
    @Retry(name = "qrCodeService")
    public QRCode getQRCode(Long id) {
        try {
            return qrCodeMapper.toDomain(qrCodeApi.getById(id));
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("QR code not found: id={}", id);
            throw new QRCodeNotFoundException("QR code not found with id: " + id);
        } catch (RestClientResponseException e) {
            log.error("Error calling QR code service for id: {}, status={}", id, e.getStatusCode(), e);
            throw new QRCodeNotFoundException("Failed to retrieve QR code: " + e.getMessage());
        }
    }

    @CircuitBreaker(name = "qrCodeService")
    @Retry(name = "qrCodeService")
    public QRCode validateQRCode(String code) {
        try {
            return qrCodeMapper.toDomain(qrCodeApi.validate(new ValidateQRCodeRequest(code)));
        } catch (RestClientResponseException e) {
            log.error("Error validating QR code: {}, status={}", code, e.getStatusCode(), e);
            throw new RuntimeException("Failed to validate QR code: " + e.getMessage(), e);
        }
    }
}
