package com.example.paymentservice.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Resilience4j configuration for inter-service HTTP calls.
 *
 * Circuit Breaker: Prevents cascading failures by short-circuiting calls
 * to downstream services that are consistently failing.
 *
 * Retry: Automatically retries transient failures (network blips, 503s)
 * with exponential backoff.
 *
 * These are instance-local (not shared across pods), which is the standard
 * approach — each instance independently monitors its own downstream health.
 */
@Configuration
public class ResilienceConfig {

    // --- Circuit Breaker defaults ---
    @Value("${resilience.circuitbreaker.failure-rate-threshold:50}")
    private float failureRateThreshold;

    @Value("${resilience.circuitbreaker.sliding-window-size:10}")
    private int slidingWindowSize;

    @Value("${resilience.circuitbreaker.minimum-number-of-calls:5}")
    private int minimumNumberOfCalls;

    @Value("${resilience.circuitbreaker.wait-duration-in-open-state:30000}")
    private long waitDurationInOpenStateMs;

    @Value("${resilience.circuitbreaker.permitted-calls-in-half-open:3}")
    private int permittedCallsInHalfOpen;

    // --- Retry defaults ---
    @Value("${resilience.retry.max-attempts:3}")
    private int maxAttempts;

    @Value("${resilience.retry.wait-duration:500}")
    private long waitDurationMs;

    @Bean
    public CircuitBreakerConfig circuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(failureRateThreshold)
                .slidingWindowSize(slidingWindowSize)
                .minimumNumberOfCalls(minimumNumberOfCalls)
                .waitDurationInOpenState(Duration.ofMillis(waitDurationInOpenStateMs))
                .permittedNumberOfCallsInHalfOpenState(permittedCallsInHalfOpen)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();
    }

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry(CircuitBreakerConfig config) {
        return CircuitBreakerRegistry.of(config);
    }

    @Bean
    public RetryConfig retryConfig() {
        return RetryConfig.custom()
                .maxAttempts(maxAttempts)
                .waitDuration(Duration.ofMillis(waitDurationMs))
                .retryExceptions(Exception.class)
                .build();
    }

    @Bean
    public RetryRegistry retryRegistry(RetryConfig config) {
        return RetryRegistry.of(config);
    }

    // Named circuit breakers for each downstream service
    @Bean
    public CircuitBreaker walletServiceCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("walletService");
    }

    @Bean
    public CircuitBreaker qrCodeServiceCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("qrCodeService");
    }

    @Bean
    public CircuitBreaker userServiceCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("userService");
    }

    // Named retry instances for each downstream service
    @Bean
    public Retry walletServiceRetry(RetryRegistry registry) {
        return registry.retry("walletService");
    }

    @Bean
    public Retry qrCodeServiceRetry(RetryRegistry registry) {
        return registry.retry("qrCodeService");
    }

    @Bean
    public Retry userServiceRetry(RetryRegistry registry) {
        return registry.retry("userService");
    }
}
