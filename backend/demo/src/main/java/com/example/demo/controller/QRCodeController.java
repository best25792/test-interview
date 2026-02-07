package com.example.demo.controller;

import com.example.demo.dto.request.CreateQRCodeRequest;
import com.example.demo.dto.response.QRCodeResponse;
import com.example.demo.entity.QRCode;
import com.example.demo.entity.QRCodeStatus;
import com.example.demo.exception.QRCodeNotFoundException;
import com.example.demo.repository.qr.QRCodeRepository;
import com.example.demo.service.QRCodeService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/qrcodes")
@RequiredArgsConstructor
@Slf4j
public class QRCodeController {

    private final QRCodeRepository qrCodeRepository;
    private final QRCodeService qrCodeService;

    @GetMapping("/{id}")
    public ResponseEntity<QRCodeResponse> getQRCode(@PathVariable Long id) {
        QRCode qrCode = qrCodeRepository.findById(id)
                .orElseThrow(() -> new QRCodeNotFoundException("QR code not found with id: " + id));
        
        QRCodeResponse response = mapToQRCodeResponse(qrCode);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<QRCodeResponse> createQRCode(@Valid @RequestBody CreateQRCodeRequest request) {
        // Deactivate any existing active QR codes for the same payment
        int deactivatedByPayment = qrCodeRepository.deactivateByPaymentId(
                request.getPaymentId(),
                QRCodeStatus.ACTIVE,
                QRCodeStatus.CANCELLED
        );
        
        if (deactivatedByPayment > 0) {
            log.info("Deactivated {} existing active QR code(s) for paymentId: {}", 
                    deactivatedByPayment, request.getPaymentId());
        }
        
        // Generate new QR code
        String qrCodeData = generateUniqueQRCode(request.getPaymentId());
        
        QRCode qrCode = QRCode.builder()
                .code(qrCodeData)
                .paymentId(request.getPaymentId())
                .status(QRCodeStatus.ACTIVE)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();
        
        QRCode savedQRCode = qrCodeRepository.save(qrCode);
        
        QRCodeResponse response = mapToQRCodeResponse(savedQRCode);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/validate")
    public ResponseEntity<QRCodeResponse> validateQRCode(@Valid @RequestBody ValidateQRCodeRequest request) {
        QRCode qrCode = qrCodeService.validateQRCode(request.getCode());
        QRCodeResponse response = mapToQRCodeResponse(qrCode);
        return ResponseEntity.ok(response);
    }
    
    private String generateUniqueQRCode(Long paymentId) {
        // Generate a unique QR code string with timestamp and UUID for better uniqueness
        long timestamp = System.currentTimeMillis();
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return "PAYMENT_" + paymentId + "_" + timestamp + "_" + uuid.substring(0, 16);
    }

    private QRCodeResponse mapToQRCodeResponse(QRCode qrCode) {
        return QRCodeResponse.builder()
                .id(qrCode.getId())
                .code(qrCode.getCode())
                .paymentId(qrCode.getPaymentId())
                .status(qrCode.getStatus())
                .expiresAt(qrCode.getExpiresAt())
                .createdAt(qrCode.getCreatedAt())
                .build();
    }

    @Data
    static class ValidateQRCodeRequest {
        private String code;
    }
}
