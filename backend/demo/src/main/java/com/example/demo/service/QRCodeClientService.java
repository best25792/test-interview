package com.example.demo.service;

import com.example.demo.dto.request.CreateQRCodeRequest;
import com.example.demo.dto.request.ValidateQRCodeRequest;
import com.example.demo.dto.response.QRCodeResponse;
import com.example.demo.exception.QRCodeNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Service that uses RestClient to call QR Code Service
 * This abstracts the HTTP calls and provides a clean interface
 */
@Service
@Slf4j
public class QRCodeClientService {

    private final RestClient restClient;

    public QRCodeClientService(RestClient.Builder restClientBuilder,@Value("${qr.code.service.url:http://localhost:8080}") String baseUrl) {
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .build();
    }

    /**
     * Create QR code for a payment using RestClient
     * @param paymentId Payment ID
     * @param customerId Optional customer ID for deactivating existing QR codes
     * @return QRCodeResponse
     * @throws RuntimeException if creation fails
     */
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
     * @param id QR code ID
     * @return QRCodeResponse
     * @throws QRCodeNotFoundException if QR code not found
     */
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
     * @param code QR code string
     * @return QRCodeResponse
     * @throws RuntimeException if validation fails
     */
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
