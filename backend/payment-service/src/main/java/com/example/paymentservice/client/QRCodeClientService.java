package com.example.paymentservice.client;

import com.example.paymentservice.dto.request.CreateQRCodeRequest;
import com.example.paymentservice.dto.request.ValidateQRCodeRequest;
import com.example.paymentservice.dto.response.QRCodeResponse;
import com.example.paymentservice.exception.QRCodeNotFoundException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * REST client for QR Code Service.
 *
 * Protected by Resilience4j annotations:
 * - @CircuitBreaker: opens after 50% failure rate, fails fast with CallNotPermittedException
 * - @Retry: retries transient failures up to 3 times with 500ms backoff
 * - QRCodeNotFoundException is ignored by both (business error, not infrastructure)
 */
@Service
@Slf4j
public class QRCodeClientService {

    private final RestClient restClient;

    public QRCodeClientService(RestClient.Builder restClientBuilder,
                               @Value("${qr.code.service.url:http://localhost:8084}") String baseUrl) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    /**
     * Create QR code for a payment using RestClient
     */
    @CircuitBreaker(name = "qrCodeService")
    @Retry(name = "qrCodeService")
    public QRCodeResponse createQRCode(Long paymentId, String customerId) {
        try {
            CreateQRCodeRequest request = new CreateQRCodeRequest(paymentId, customerId);
            QRCodeResponse response = restClient.post()
                    .uri("/api/v1/qrcodes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        log.warn("QR code creation failed with client error: status={}, paymentId={}",
                                res.getStatusCode(), paymentId);
                        throw new RuntimeException("QR code creation failed: " + res.getStatusCode());
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        log.error("QR code service returned server error: status={}, paymentId={}",
                                res.getStatusCode(), paymentId);
                        throw new RuntimeException("QR code service error: " + res.getStatusCode());
                    })
                    .body(QRCodeResponse.class);

            log.info("QR code created successfully via RestClient: paymentId={}, qrCodeId={}",
                    paymentId, response.getId());
            return response;
        } catch (RestClientException e) {
            log.error("Error creating QR code via RestClient: paymentId={}", paymentId, e);
            throw new RuntimeException("Failed to create QR code: " + e.getMessage(), e);
        }
    }

    /**
     * Get QR code by ID using RestClient
     */
    @CircuitBreaker(name = "qrCodeService")
    @Retry(name = "qrCodeService")
    public QRCodeResponse getQRCode(Long id) {
        try {
            QRCodeResponse response = restClient.get()
                    .uri("/api/v1/qrcodes/{id}", id)
                    .retrieve()
                    .onStatus(status -> status == HttpStatus.NOT_FOUND, (req, res) -> {
                        log.warn("QR code not found: id={}", id);
                        throw new QRCodeNotFoundException("QR code not found with id: " + id);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        log.error("QR code service returned server error: status={}, id={}",
                                res.getStatusCode(), id);
                        throw new RuntimeException("QR code service error: " + res.getStatusCode());
                    })
                    .body(QRCodeResponse.class);

            return response;
        } catch (QRCodeNotFoundException e) {
            throw e;
        } catch (RestClientException e) {
            log.error("Error calling QR code service for id: {}", id, e);
            throw new QRCodeNotFoundException("Failed to retrieve QR code: " + e.getMessage());
        }
    }

    /**
     * Validate QR code using RestClient
     */
    @CircuitBreaker(name = "qrCodeService")
    @Retry(name = "qrCodeService")
    public QRCodeResponse validateQRCode(String code) {
        try {
            ValidateQRCodeRequest request = new ValidateQRCodeRequest(code);
            QRCodeResponse response = restClient.post()
                    .uri("/api/v1/qrcodes/validate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        log.warn("QR code validation failed with client error: status={}, code={}",
                                res.getStatusCode(), code);
                        throw new RuntimeException("QR code validation failed: " + res.getStatusCode());
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        log.error("QR code service returned server error: status={}, code={}",
                                res.getStatusCode(), code);
                        throw new RuntimeException("QR code service error: " + res.getStatusCode());
                    })
                    .body(QRCodeResponse.class);

            return response;
        } catch (RestClientException e) {
            log.error("Error validating QR code: {}", code, e);
            throw new RuntimeException("Failed to validate QR code: " + e.getMessage(), e);
        }
    }
}
