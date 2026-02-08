package com.example.paymentservice.client;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Set;

/**
 * REST client for Wallet Service.
 * Provides atomic wallet operations (deduct, top-up) without hold/capture flow.
 *
 * Protected by Resilience4j Circuit Breaker + Retry:
 * - Transient failures (network blips, 503) are retried automatically
 * - Persistent failures trigger circuit breaker to fail fast
 * - Business errors (InsufficientBalance) are NOT retried or counted as failures
 */
@Service
@Slf4j
public class WalletClientService {

    private final RestClient.Builder restClientBuilder;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    @Value("${wallet.service.url:http://localhost:8082}")
    private String baseUrl;

    private RestClient restClient;

    public WalletClientService(RestClient.Builder restClientBuilder,
                               @Qualifier("walletServiceCircuitBreaker") CircuitBreaker circuitBreaker,
                               @Qualifier("walletServiceRetry") Retry retry) {
        this.restClientBuilder = restClientBuilder;
        this.circuitBreaker = circuitBreaker;
        this.retry = retry;
    }

    private RestClient getRestClient() {
        if (restClient == null) {
            restClient = restClientBuilder.baseUrl(baseUrl).build();
        }
        return restClient;
    }

    /**
     * Deduct amount from user wallet (atomic check + deduct).
     * Throws InsufficientBalanceException if balance is not enough.
     * Throws ServiceUnavailableException if circuit breaker is open.
     */
    public void deductFromWallet(Long userId, BigDecimal amount) {
        ResilientCall.executeRunnable(circuitBreaker, retry, () -> {
            doDeductFromWallet(userId, amount);
        }, Set.of(InsufficientBalanceException.class));
    }

    private void doDeductFromWallet(Long userId, BigDecimal amount) {
        try {
            getRestClient()
                    .post()
                    .uri("/api/v1/wallets/users/{userId}/deduct", userId)
                    .body(new DeductRequest(amount))
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        log.error("Wallet deduction failed for userId: {}, amount: {}, status: {}",
                                userId, amount, res.getStatusCode());
                        throw new InsufficientBalanceException(
                                "Wallet deduction failed for userId: " + userId + ", status: " + res.getStatusCode());
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        log.error("Wallet service error for userId: {}, status: {}", userId, res.getStatusCode());
                        throw new WalletServiceException(
                                "Wallet service error: " + res.getStatusCode());
                    })
                    .toBodilessEntity();
        } catch (InsufficientBalanceException | WalletServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error calling Wallet Service to deduct, userId: {}, amount: {}", userId, amount, e);
            throw new WalletServiceException("Failed to deduct from wallet", e);
        }
    }

    /**
     * Add amount to wallet (for refunds, top-ups, etc.)
     * Throws ServiceUnavailableException if circuit breaker is open.
     */
    public void addToWallet(Long userId, BigDecimal amount, String reason) {
        ResilientCall.executeRunnable(circuitBreaker, retry, () -> {
            doAddToWallet(userId, amount, reason);
        }, Set.of());
    }

    private void doAddToWallet(Long userId, BigDecimal amount, String reason) {
        try {
            getRestClient()
                    .post()
                    .uri("/api/v1/wallets/users/{userId}/topup", userId)
                    .body(new TopUpRequest(amount))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        log.error("Failed to add to wallet, userId: {}, amount: {}, status: {}",
                                userId, amount, res.getStatusCode());
                        throw new WalletServiceException(
                                "Failed to add to wallet: " + res.getStatusCode());
                    })
                    .toBodilessEntity();
        } catch (WalletServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error calling Wallet Service to add to wallet, userId: {}, amount: {}", userId, amount, e);
            throw new WalletServiceException("Failed to add to wallet", e);
        }
    }

    // DTOs
    public record DeductRequest(BigDecimal amount) {}
    public record TopUpRequest(BigDecimal amount) {}

    // Exceptions
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
