package com.example.paymentservice.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * REST client for User Service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserClientService {

    private final RestClient.Builder restClientBuilder;
    
    @Value("${user.service.url:http://localhost:8081}")
    private String baseUrl;
    
    private RestClient restClient;

    private RestClient getRestClient() {
        if (restClient == null) {
            restClient = restClientBuilder.baseUrl(baseUrl).build();
        }
        return restClient;
    }

    /**
     * Get user by ID
     */
    public UserResponse getUser(Long userId) {
        try {
            return getRestClient()
                    .get()
                    .uri("/api/v1/users/{id}", userId)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        log.error("Failed to get user, userId: {}, status: {}", 
                                userId, res.getStatusCode());
                        throw new RuntimeException("Failed to get user: " + res.getStatusCode());
                    })
                    .body(new ParameterizedTypeReference<UserResponse>() {});
        } catch (Exception e) {
            log.error("Error calling User Service for user, userId: {}", userId, e);
            throw new RuntimeException("Failed to get user", e);
        }
    }

    /**
     * Get wallet from User Service (which calls Wallet Service)
     */
    public WalletResponse getWallet(Long userId) {
        try {
            return getRestClient()
                    .get()
                    .uri("/api/v1/users/{id}/wallet", userId)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        log.error("Failed to get wallet, userId: {}, status: {}", 
                                userId, res.getStatusCode());
                        throw new RuntimeException("Failed to get wallet: " + res.getStatusCode());
                    })
                    .body(new ParameterizedTypeReference<WalletResponse>() {});
        } catch (Exception e) {
            log.error("Error calling User Service for wallet, userId: {}", userId, e);
            throw new RuntimeException("Failed to get wallet", e);
        }
    }

    /**
     * Validate user conditions (user active and wallet active)
     */
    public boolean validateUserConditions(Long userId) {
        try {
            UserResponse user = getUser(userId);
            if (!user.isActive()) {
                log.warn("User {} is not active", userId);
                return false;
            }
            
            WalletResponse wallet = getWallet(userId);
            if (!wallet.isActive()) {
                log.warn("Wallet for user {} is not active", userId);
                return false;
            }
            
            return true;
        } catch (Exception e) {
            log.error("Error validating user conditions, userId: {}", userId, e);
            return false;
        }
    }

    // DTOs
    public record UserResponse(Long id, String username, String email, Boolean isActive, Boolean isVerified) {}
    public record WalletResponse(Long id, Long userId, java.math.BigDecimal balance, String currency, Boolean isActive, 
                         java.time.LocalDateTime createdAt, java.time.LocalDateTime updatedAt) {}
}
