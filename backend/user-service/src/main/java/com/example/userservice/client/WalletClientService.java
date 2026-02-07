package com.example.userservice.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

/**
 * REST client for Wallet Service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WalletClientService {

    private final RestClient.Builder restClientBuilder;
    
    @Value("${wallet.service.url:http://localhost:8082}")
    private String baseUrl;
    
    private RestClient restClient;

    private RestClient getRestClient() {
        if (restClient == null) {
            restClient = restClientBuilder.baseUrl(baseUrl).build();
        }
        return restClient;
    }

    /**
     * Get wallet balance from Wallet Service
     */
    public BigDecimal getWalletBalance(Long userId) {
        try {
            WalletBalanceResponse apiResponse = getRestClient()
                    .get()
                    .uri("/api/v1/wallets/users/{userId}/balance", userId)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), 
                            (request, response) -> {
                                log.error("Failed to get wallet balance for userId: {}, status: {}", 
                                        userId, response.getStatusCode());
                                throw new RuntimeException("Failed to get wallet balance: " + response.getStatusCode());
                            })
                    .body(new ParameterizedTypeReference<WalletBalanceResponse>() {});
            
            return apiResponse != null ? apiResponse.balance() : BigDecimal.ZERO;
        } catch (Exception e) {
            log.error("Error calling Wallet Service for wallet balance, userId: {}", userId, e);
            throw new RuntimeException("Failed to get wallet balance from Wallet Service", e);
        }
    }

    /**
     * Get wallet from Wallet Service
     */
    public WalletResponse getWallet(Long userId) {
        try {
            return getRestClient()
                    .get()
                    .uri("/api/v1/wallets/users/{userId}", userId)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            (request, response) -> {
                                log.error("Failed to get wallet for userId: {}, status: {}", 
                                        userId, response.getStatusCode());
                                throw new RuntimeException("Failed to get wallet: " + response.getStatusCode());
                            })
                    .body(new ParameterizedTypeReference<WalletResponse>() {});
        } catch (Exception e) {
            log.error("Error calling Wallet Service for wallet, userId: {}", userId, e);
            throw new RuntimeException("Failed to get wallet from Wallet Service", e);
        }
    }

    /**
     * Top up wallet in Wallet Service
     */
    public BigDecimal topUpWallet(Long userId, BigDecimal amount) {
        try {
            WalletBalanceResponse apiResponse = getRestClient()
                    .post()
                    .uri("/api/v1/wallets/users/{userId}/topup", userId)
                    .body(new TopUpRequest(amount))
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            (request, response) -> {
                                log.error("Failed to top up wallet, userId: {}, amount: {}, status: {}", 
                                        userId, amount, response.getStatusCode());
                                throw new RuntimeException("Failed to top up wallet: " + response.getStatusCode());
                            })
                    .body(new ParameterizedTypeReference<WalletBalanceResponse>() {});
            
            return apiResponse != null ? apiResponse.balance() : BigDecimal.ZERO;
        } catch (Exception e) {
            log.error("Error calling Wallet Service to top up wallet, userId: {}, amount: {}", userId, amount, e);
            throw new RuntimeException("Failed to top up wallet in Wallet Service", e);
        }
    }

    public record WalletBalanceResponse(Long userId, BigDecimal balance) {}
    public record WalletResponse(Long id, Long userId, BigDecimal balance, String currency, Boolean isActive, 
                         java.time.LocalDateTime createdAt, java.time.LocalDateTime updatedAt) {}
    public record TopUpRequest(BigDecimal amount) {}
}
