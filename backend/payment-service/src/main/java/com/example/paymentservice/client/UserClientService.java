package com.example.paymentservice.client;

import com.example.paymentservice.client.api.UserApi;
import com.example.paymentservice.domain.model.User;
import com.example.paymentservice.domain.model.Wallet;
import com.example.paymentservice.mapper.UserMapper;
import com.example.paymentservice.mapper.WalletMapper;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

/**
 * Wrapper around UserApi (HTTP Service client). Returns domain objects;
 * adds Resilience4j and validateUserConditions logic.
 */
@Service
@Slf4j
public class UserClientService {

    private final UserApi userApi;
    private final UserMapper userMapper;
    private final WalletMapper walletMapper;

    public UserClientService(UserApi userApi, UserMapper userMapper, WalletMapper walletMapper) {
        this.userApi = userApi;
        this.userMapper = userMapper;
        this.walletMapper = walletMapper;
    }

    @CircuitBreaker(name = "userService")
    @Retry(name = "userService")
    public User getUser(Long userId) {
        try {
            return userMapper.toDomain(userApi.getUser(userId));
        } catch (RestClientResponseException e) {
            log.error("Failed to get user, userId: {}, status: {}", userId, e.getStatusCode(), e);
            throw new RuntimeException("Failed to get user: " + e.getStatusCode(), e);
        }
    }

    @CircuitBreaker(name = "userService")
    @Retry(name = "userService")
    public Wallet getWallet(Long userId) {
        try {
            return walletMapper.toDomain(userApi.getWallet(userId));
        } catch (RestClientResponseException e) {
            log.error("Failed to get wallet, userId: {}, status: {}", userId, e.getStatusCode(), e);
            throw new RuntimeException("Failed to get wallet: " + e.getStatusCode(), e);
        }
    }

    public boolean validateUserConditions(Long userId) {
        try {
            User user = getUser(userId);
            if (!user.isActive()) {
                log.warn("User {} is not active", userId);
                return false;
            }

            Wallet wallet = getWallet(userId);
            if (!wallet.isActive()) {
                log.warn("Wallet for user {} is not active", userId);
                return false;
            }

            return true;
        } catch (CallNotPermittedException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error validating user conditions, userId: {}", userId, e);
            return false;
        }
    }
}
