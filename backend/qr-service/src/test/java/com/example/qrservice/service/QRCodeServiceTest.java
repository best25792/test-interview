package com.example.qrservice.service;

import com.example.qrservice.entity.QRCode;
import com.example.qrservice.entity.QRCodeStatus;
import com.example.qrservice.repository.QRCodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QRCodeServiceTest {

    @Mock
    private QRCodeRepository qrCodeRepository;

    @InjectMocks
    private QRCodeService qrCodeService;

    private Long paymentId = 100L;
    private QRCode activeQRCode;
    private QRCode expiredQRCode;

    @BeforeEach
    void setUp() {
        activeQRCode = QRCode.builder()
                .id(1L)
                .code("PAYMENT_100_1234567890_ABCDEF123456")
                .paymentId(paymentId)
                .status(QRCodeStatus.ACTIVE)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .createdAt(LocalDateTime.now())
                .build();

        expiredQRCode = QRCode.builder()
                .id(2L)
                .code("PAYMENT_100_EXPIRED_CODE")
                .paymentId(paymentId)
                .status(QRCodeStatus.ACTIVE)
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .createdAt(LocalDateTime.now().minusMinutes(20))
                .build();
    }

    @Test
    void testCreateQRCode_Success() {
        // Given
        when(qrCodeRepository.deactivateByPaymentId(paymentId, QRCodeStatus.ACTIVE, QRCodeStatus.CANCELLED))
                .thenReturn(0);
        when(qrCodeRepository.save(any(QRCode.class))).thenAnswer(invocation -> {
            QRCode qrCode = invocation.getArgument(0);
            qrCode.setId(1L);
            return qrCode;
        });

        // When
        QRCode result = qrCodeService.createQRCode(paymentId, "CUSTOMER_001");

        // Then
        assertNotNull(result);
        assertEquals(paymentId, result.getPaymentId());
        assertEquals(QRCodeStatus.ACTIVE, result.getStatus());
        assertNotNull(result.getCode());
        assertTrue(result.getCode().startsWith("PAYMENT_" + paymentId));
        verify(qrCodeRepository).deactivateByPaymentId(paymentId, QRCodeStatus.ACTIVE, QRCodeStatus.CANCELLED);
        verify(qrCodeRepository).save(any(QRCode.class));
    }

    @Test
    void testCreateQRCode_DeactivatesExisting() {
        // Given
        when(qrCodeRepository.deactivateByPaymentId(paymentId, QRCodeStatus.ACTIVE, QRCodeStatus.CANCELLED))
                .thenReturn(2); // 2 existing QR codes deactivated
        when(qrCodeRepository.save(any(QRCode.class))).thenAnswer(invocation -> {
            QRCode qrCode = invocation.getArgument(0);
            qrCode.setId(1L);
            return qrCode;
        });

        // When
        QRCode result = qrCodeService.createQRCode(paymentId, "CUSTOMER_001");

        // Then
        assertNotNull(result);
        verify(qrCodeRepository).deactivateByPaymentId(paymentId, QRCodeStatus.ACTIVE, QRCodeStatus.CANCELLED);
        verify(qrCodeRepository).save(any(QRCode.class));
    }

    @Test
    void testValidateQRCode_Success() {
        // Given
        String code = "PAYMENT_100_1234567890_ABCDEF123456";
        when(qrCodeRepository.findByCode(code)).thenReturn(Optional.of(activeQRCode));

        // When
        QRCode result = qrCodeService.validateQRCode(code);

        // Then
        assertNotNull(result);
        assertEquals(QRCodeStatus.ACTIVE, result.getStatus());
        verify(qrCodeRepository).findByCode(code);
        verify(qrCodeRepository, never()).save(any(QRCode.class));
    }

    @Test
    void testValidateQRCode_NotFound() {
        // Given
        String code = "INVALID_CODE";
        when(qrCodeRepository.findByCode(code)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            qrCodeService.validateQRCode(code);
        });

        assertTrue(exception.getMessage().contains("QR code not found"));
        verify(qrCodeRepository).findByCode(code);
    }

    @Test
    void testValidateQRCode_NotActive() {
        // Given
        String code = "PAYMENT_100_1234567890_ABCDEF123456";
        activeQRCode.setStatus(QRCodeStatus.USED);
        when(qrCodeRepository.findByCode(code)).thenReturn(Optional.of(activeQRCode));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            qrCodeService.validateQRCode(code);
        });

        assertTrue(exception.getMessage().contains("QR code is not active"));
    }

    @Test
    void testValidateQRCode_Expired() {
        // Given
        String code = "PAYMENT_100_EXPIRED_CODE";
        when(qrCodeRepository.findByCode(code)).thenReturn(Optional.of(expiredQRCode));
        when(qrCodeRepository.save(any(QRCode.class))).thenReturn(expiredQRCode);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            qrCodeService.validateQRCode(code);
        });

        assertTrue(exception.getMessage().contains("QR code has expired"));
        assertEquals(QRCodeStatus.EXPIRED, expiredQRCode.getStatus());
        verify(qrCodeRepository).save(expiredQRCode);
    }

    @Test
    void testMarkQRCodeAsUsed_Success() {
        // Given
        Long qrCodeId = 1L;
        when(qrCodeRepository.findById(qrCodeId)).thenReturn(Optional.of(activeQRCode));
        when(qrCodeRepository.save(any(QRCode.class))).thenReturn(activeQRCode);

        // When
        qrCodeService.markQRCodeAsUsed(qrCodeId);

        // Then
        assertEquals(QRCodeStatus.USED, activeQRCode.getStatus());
        verify(qrCodeRepository).findById(qrCodeId);
        verify(qrCodeRepository).save(activeQRCode);
    }

    @Test
    void testMarkQRCodeAsUsed_NotFound() {
        // Given
        Long qrCodeId = 999L;
        when(qrCodeRepository.findById(qrCodeId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            qrCodeService.markQRCodeAsUsed(qrCodeId);
        });

        assertTrue(exception.getMessage().contains("QR code not found"));
        verify(qrCodeRepository).findById(qrCodeId);
        verify(qrCodeRepository, never()).save(any(QRCode.class));
    }

    @Test
    void testDeactivateActiveQRCodesForPayment() {
        // Given
        when(qrCodeRepository.deactivateByPaymentId(paymentId, QRCodeStatus.ACTIVE, QRCodeStatus.CANCELLED))
                .thenReturn(3);

        // When
        qrCodeService.deactivateActiveQRCodesForPayment(paymentId);

        // Then
        verify(qrCodeRepository).deactivateByPaymentId(paymentId, QRCodeStatus.ACTIVE, QRCodeStatus.CANCELLED);
    }
}
