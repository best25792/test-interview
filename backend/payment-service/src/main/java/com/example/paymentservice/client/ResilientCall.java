package com.example.paymentservice.client;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.function.Supplier;

/**
 * Utility to compose Resilience4j Circuit Breaker + Retry around a supplier.
 * 
 * Execution order: Retry wraps CircuitBreaker wraps the actual call.
 *   Retry → CircuitBreaker → HTTP call
 * 
 * This means:
 *   1. Retry will re-execute when CircuitBreaker lets a call through that fails
 *   2. If CircuitBreaker is OPEN, CallNotPermittedException is thrown immediately
 *      and Retry will NOT retry (because it's a non-retryable exception)
 */
@Slf4j
public final class ResilientCall {

    private ResilientCall() {}

    /**
     * Execute a supplier with circuit breaker + retry protection.
     *
     * @param circuitBreaker the circuit breaker instance
     * @param retry          the retry instance
     * @param supplier       the actual HTTP call
     * @param ignoredExceptions exceptions that should NOT trip the circuit breaker
     *                          (e.g. business errors like InsufficientBalanceException)
     * @param <T> return type
     * @return the result from the supplier
     */
    public static <T> T execute(CircuitBreaker circuitBreaker,
                                Retry retry,
                                Supplier<T> supplier,
                                Set<Class<? extends Exception>> ignoredExceptions) {
        // Decorate: Retry( CircuitBreaker( supplier ) )
        Supplier<T> decorated = CircuitBreaker.decorateSupplier(circuitBreaker, supplier);
        decorated = Retry.decorateSupplier(retry, decorated);

        try {
            return decorated.get();
        } catch (CallNotPermittedException e) {
            log.warn("Circuit breaker [{}] is OPEN — call rejected. State: {}",
                    circuitBreaker.getName(), circuitBreaker.getState());
            throw new ServiceUnavailableException(
                    "Service " + circuitBreaker.getName() + " is currently unavailable (circuit open)", e);
        }
    }

    /**
     * Execute a runnable (void return) with circuit breaker + retry protection.
     */
    public static void executeRunnable(CircuitBreaker circuitBreaker,
                                       Retry retry,
                                       Runnable runnable,
                                       Set<Class<? extends Exception>> ignoredExceptions) {
        execute(circuitBreaker, retry, () -> {
            runnable.run();
            return null;
        }, ignoredExceptions);
    }
}
