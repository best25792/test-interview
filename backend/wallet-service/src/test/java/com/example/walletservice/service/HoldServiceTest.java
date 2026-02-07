package com.example.walletservice.service;

import com.example.walletservice.entity.Hold;
import com.example.walletservice.entity.HoldStatus;
import com.example.walletservice.exception.PaymentException;
import com.example.walletservice.repository.HoldRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class HoldServiceTest {

    @Mock
    private HoldRepository holdRepository;

    @Mock
    private WalletService walletService;

    @InjectMocks
    private HoldService holdService;

    private Long userId = 1L;
    private Long paymentId = 100L;
    private BigDecimal walletBalance = new BigDecimal("100.00");

    @BeforeEach
    void setUp() {
        // Setup common mocks that are used by most tests
        lenient().when(walletService.getWalletBalance(userId)).thenReturn(walletBalance);
        lenient().when(holdRepository.getTotalHeldAmountByUserAndStatus(userId, HoldStatus.ACTIVE))
                .thenReturn(BigDecimal.ZERO);
    }

    @Test
    void testGetAvailableBalance_NoActiveHolds() {
        // Given
        when(holdRepository.getTotalHeldAmountByUserAndStatus(userId, HoldStatus.ACTIVE))
                .thenReturn(BigDecimal.ZERO);

        // When
        BigDecimal availableBalance = holdService.getAvailableBalance(userId);

        // Then
        assertEquals(walletBalance, availableBalance);
        verify(walletService).getWalletBalance(userId);
        verify(holdRepository).getTotalHeldAmountByUserAndStatus(userId, HoldStatus.ACTIVE);
    }

    @Test
    void testGetAvailableBalance_WithActiveHolds() {
        // Given
        BigDecimal heldAmount = new BigDecimal("30.00");
        when(holdRepository.getTotalHeldAmountByUserAndStatus(userId, HoldStatus.ACTIVE))
                .thenReturn(heldAmount);

        // When
        BigDecimal availableBalance = holdService.getAvailableBalance(userId);

        // Then
        assertEquals(new BigDecimal("70.00"), availableBalance);
    }

    @Test
    void testCreateHold_Success() {
        // Given
        BigDecimal amount = new BigDecimal("50.00");
        String reason = "Payment authorization";
        when(holdRepository.getTotalHeldAmountByUserAndStatus(userId, HoldStatus.ACTIVE))
                .thenReturn(BigDecimal.ZERO);
        when(holdRepository.save(any(Hold.class))).thenAnswer(invocation -> {
            Hold hold = invocation.getArgument(0);
            hold.setId(1L);
            return hold;
        });

        // When
        Hold result = holdService.createHold(userId, paymentId, amount, reason);

        // Then
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(paymentId, result.getPaymentId());
        assertEquals(amount, result.getAmount());
        assertEquals(HoldStatus.ACTIVE, result.getStatus());
        verify(holdRepository).save(any(Hold.class));
    }

    @Test
    void testCreateHold_InsufficientBalance() {
        // Given
        BigDecimal amount = new BigDecimal("150.00");
        BigDecimal heldAmount = new BigDecimal("30.00");
        when(holdRepository.getTotalHeldAmountByUserAndStatus(userId, HoldStatus.ACTIVE))
                .thenReturn(heldAmount);

        // When & Then
        PaymentException exception = assertThrows(PaymentException.class, () -> {
            holdService.createHold(userId, paymentId, amount, "Payment");
        });

        assertTrue(exception.getMessage().contains("Insufficient available balance"));
        verify(holdRepository, never()).save(any(Hold.class));
    }

    @Test
    void testCaptureHold_Success() {
        // Given
        Long holdId = 1L;
        BigDecimal amount = new BigDecimal("50.00");
        Hold hold = Hold.builder()
                .id(holdId)
                .userId(userId)
                .paymentId(paymentId)
                .amount(amount)
                .status(HoldStatus.ACTIVE)
                .build();

        when(holdRepository.findByIdWithLock(holdId)).thenReturn(Optional.of(hold));
        when(holdRepository.save(any(Hold.class))).thenReturn(hold);

        // When
        holdService.captureHold(holdId, "Payment confirmed");

        // Then
        verify(walletService).deductFromWallet(userId, amount);
        verify(holdRepository).save(any(Hold.class));
        assertEquals(HoldStatus.CAPTURED, hold.getStatus());
        assertNotNull(hold.getCapturedAt());
    }

    @Test
    void testCaptureHold_HoldNotFound() {
        // Given
        Long holdId = 999L;
        when(holdRepository.findByIdWithLock(holdId)).thenReturn(Optional.empty());

        // When & Then
        PaymentException exception = assertThrows(PaymentException.class, () -> {
            holdService.captureHold(holdId, "Payment confirmed");
        });

        assertEquals("Hold not found with id: " + holdId, exception.getMessage());
        verify(walletService, never()).deductFromWallet(anyLong(), any());
    }

    @Test
    void testCaptureHold_HoldNotActive() {
        // Given
        Long holdId = 1L;
        Hold hold = Hold.builder()
                .id(holdId)
                .userId(userId)
                .paymentId(paymentId)
                .amount(new BigDecimal("50.00"))
                .status(HoldStatus.RELEASED)
                .build();

        when(holdRepository.findByIdWithLock(holdId)).thenReturn(Optional.of(hold));

        // When & Then
        PaymentException exception = assertThrows(PaymentException.class, () -> {
            holdService.captureHold(holdId, "Payment confirmed");
        });

        assertTrue(exception.getMessage().contains("Hold is not active"));
        verify(walletService, never()).deductFromWallet(anyLong(), any());
    }

    @Test
    void testReleaseHold_Success() {
        // Given
        Long holdId = 1L;
        Hold hold = Hold.builder()
                .id(holdId)
                .userId(userId)
                .paymentId(paymentId)
                .amount(new BigDecimal("50.00"))
                .status(HoldStatus.ACTIVE)
                .build();

        when(holdRepository.findByIdWithLock(holdId)).thenReturn(Optional.of(hold));
        when(holdRepository.save(any(Hold.class))).thenReturn(hold);

        // When
        holdService.releaseHold(holdId, "Payment cancelled");

        // Then
        verify(holdRepository).save(any(Hold.class));
        assertEquals(HoldStatus.RELEASED, hold.getStatus());
        assertNotNull(hold.getReleasedAt());
    }

    @Test
    void testReleaseHold_NonActiveHold() {
        // Given
        Long holdId = 1L;
        Hold hold = Hold.builder()
                .id(holdId)
                .userId(userId)
                .paymentId(paymentId)
                .amount(new BigDecimal("50.00"))
                .status(HoldStatus.CAPTURED)
                .build();

        when(holdRepository.findByIdWithLock(holdId)).thenReturn(Optional.of(hold));

        // When
        holdService.releaseHold(holdId, "Payment cancelled");

        // Then
        verify(holdRepository, never()).save(any(Hold.class));
    }

    @Test
    void testGetHoldByPaymentId_Success() {
        // Given
        Hold hold = Hold.builder()
                .id(1L)
                .userId(userId)
                .paymentId(paymentId)
                .amount(new BigDecimal("50.00"))
                .status(HoldStatus.ACTIVE)
                .build();

        when(holdRepository.findByPaymentId(paymentId)).thenReturn(Optional.of(hold));

        // When
        Hold result = holdService.getHoldByPaymentId(paymentId);

        // Then
        assertNotNull(result);
        assertEquals(paymentId, result.getPaymentId());
        verify(holdRepository).findByPaymentId(paymentId);
    }

    @Test
    void testGetHoldByPaymentId_NotFound() {
        // Given
        when(holdRepository.findByPaymentId(paymentId)).thenReturn(Optional.empty());

        // When & Then
        PaymentException exception = assertThrows(PaymentException.class, () -> {
            holdService.getHoldByPaymentId(paymentId);
        });

        assertEquals("Hold not found for paymentId: " + paymentId, exception.getMessage());
    }
}
