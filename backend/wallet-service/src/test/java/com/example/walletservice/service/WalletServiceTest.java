package com.example.walletservice.service;

import com.example.walletservice.entity.Wallet;
import com.example.walletservice.exception.PaymentException;
import com.example.walletservice.repository.WalletRepository;
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

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private WalletService walletService;

    private Wallet existingWallet;
    private Long userId = 1L;

    @BeforeEach
    void setUp() {
        existingWallet = Wallet.builder()
                .id(1L)
                .userId(userId)
                .balance(new BigDecimal("100.00"))
                .currency("USD")
                .isActive(true)
                .build();
    }

    @Test
    void testGetOrCreateWallet_ExistingWallet() {
        // Given
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(existingWallet));

        // When
        Wallet result = walletService.getOrCreateWallet(userId);

        // Then
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(new BigDecimal("100.00"), result.getBalance());
        verify(walletRepository).findByUserId(userId);
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    void testGetOrCreateWallet_CreateNewWallet() {
        // Given
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> {
            Wallet wallet = invocation.getArgument(0);
            wallet.setId(1L);
            return wallet;
        });

        // When
        Wallet result = walletService.getOrCreateWallet(userId);

        // Then
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(BigDecimal.ZERO, result.getBalance());
        assertEquals("USD", result.getCurrency());
        assertTrue(result.getIsActive());
        verify(walletRepository).findByUserId(userId);
        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    void testGetWalletBalance() {
        // Given
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(existingWallet));

        // When
        BigDecimal balance = walletService.getWalletBalance(userId);

        // Then
        assertEquals(new BigDecimal("100.00"), balance);
        verify(walletRepository).findByUserId(userId);
    }

    @Test
    void testAddToWallet_ExistingWallet() {
        // Given
        BigDecimal amount = new BigDecimal("50.00");
        when(walletRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(existingWallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        walletService.addToWallet(userId, amount, "Top-up");

        // Then
        verify(walletRepository).findByUserIdWithLock(userId);
        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    void testAddToWallet_CreateNewWallet() {
        // Given
        BigDecimal amount = new BigDecimal("50.00");
        when(walletRepository.findByUserIdWithLock(userId)).thenReturn(Optional.empty());
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> {
            Wallet wallet = invocation.getArgument(0);
            wallet.setId(1L);
            return wallet;
        });

        // When
        walletService.addToWallet(userId, amount, "Top-up");

        // Then
        verify(walletRepository).findByUserIdWithLock(userId);
        verify(walletRepository, atLeastOnce()).save(any(Wallet.class));
    }

    @Test
    void testDeductFromWallet_Success() {
        // Given
        BigDecimal amount = new BigDecimal("30.00");
        when(walletRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(existingWallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        walletService.deductFromWallet(userId, amount);

        // Then
        verify(walletRepository).findByUserIdWithLock(userId);
        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    void testDeductFromWallet_WalletNotFound() {
        // Given
        BigDecimal amount = new BigDecimal("30.00");
        when(walletRepository.findByUserIdWithLock(userId)).thenReturn(Optional.empty());

        // When & Then
        PaymentException exception = assertThrows(PaymentException.class, () -> {
            walletService.deductFromWallet(userId, amount);
        });

        assertEquals("Wallet not found for user id: " + userId, exception.getMessage());
        verify(walletRepository).findByUserIdWithLock(userId);
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    void testDeductFromWallet_InsufficientBalance() {
        // Given
        BigDecimal amount = new BigDecimal("150.00");
        when(walletRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(existingWallet));

        // When & Then
        PaymentException exception = assertThrows(PaymentException.class, () -> {
            walletService.deductFromWallet(userId, amount);
        });

        assertTrue(exception.getMessage().contains("Insufficient wallet balance"));
        verify(walletRepository).findByUserIdWithLock(userId);
        verify(walletRepository, never()).save(any(Wallet.class));
    }
}
