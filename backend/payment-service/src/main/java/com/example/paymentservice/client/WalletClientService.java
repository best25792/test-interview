package com.example.paymentservice.client;

import com.example.paymentservice.client.api.WalletApi;
import com.example.paymentservice.client.dto.DeductRequest;
import com.example.paymentservice.client.dto.TopUpRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;

/**
 * Wrapper around WalletApi (HTTP Service client). Adds Resilience4j and
 * maps RestClientResponseException to domain exceptions.
 */
@Service
@Slf4j
public class WalletClientService {

    private final WalletApi walletApi;

    public WalletClientService(WalletApi walletApi) {
        this.walletApi = walletApi;
    }

    @CircuitBreaker(name = "walletService")
    @Retry(name = "walletService")
    public void deductFromWallet(Long userId, BigDecimal amount) {
        try {
            walletApi.deduct(userId, new DeductRequest(amount));
        } catch (HttpClientErrorException e) {
            log.error("Wallet deduction failed for userId: {}, amount: {}, status: {}",
                    userId, amount, e.getStatusCode());
            throw new InsufficientBalanceException(
                    "Wallet deduction failed for userId: " + userId + ", status: " + e.getStatusCode());
        } catch (HttpServerErrorException e) {
            log.error("Wallet service error for userId: {}, status: {}", userId, e.getStatusCode());
            throw new WalletServiceException("Wallet service error: " + e.getStatusCode());
        } catch (RestClientResponseException e) {
            log.error("Error calling Wallet Service to deduct, userId: {}, amount: {}", userId, amount, e);
            throw new WalletServiceException("Failed to deduct from wallet", e);
        }
    }

    @CircuitBreaker(name = "walletService")
    @Retry(name = "walletService")
    public void addToWallet(Long userId, BigDecimal amount, String reason) {
        try {
            walletApi.topUp(userId, new TopUpRequest(amount));
        } catch (RestClientResponseException e) {
            HttpStatusCode status = e.getStatusCode();
            log.error("Failed to add to wallet, userId: {}, amount: {}, status: {}",
                    userId, amount, status);
            throw new WalletServiceException("Failed to add to wallet: " + status, e);
        }
    }

    public static class InsufficientBalanceException extends RuntimeException {
        public InsufficientBalanceException(String message) {
            super(message);
        }
    }

    public static class WalletServiceException extends RuntimeException {
        public WalletServiceException(String message) {
            super(message);
        }
        public WalletServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
