package com.example.qrservice.controller;

import com.example.qrservice.dto.request.CreateQRCodeRequest;
import com.example.qrservice.dto.request.ValidateQRCodeRequest;
import com.example.qrservice.dto.response.QRCodeResponse;
import com.example.qrservice.entity.QRCode;
import com.example.qrservice.entity.QRCodeStatus;
import com.example.qrservice.exception.QRCodeNotFoundException;
import com.example.qrservice.repository.QRCodeRepository;
import com.example.qrservice.service.QRCodeService;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
        QRCode qrCode = qrCodeService.createQRCode(request.getPaymentId(), request.getCustomerId());
        QRCodeResponse response = mapToQRCodeResponse(qrCode);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/validate")
    public ResponseEntity<QRCodeResponse> validateQRCode(@Valid @RequestBody ValidateQRCodeRequest request) {
        QRCode qrCode = qrCodeService.validateQRCode(request.getCode());
        QRCodeResponse response = mapToQRCodeResponse(qrCode);
        return ResponseEntity.ok(response);
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
}
