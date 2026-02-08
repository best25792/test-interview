package com.example.paymentservice.client;

/**
 * Thrown when a downstream service is unavailable (circuit breaker OPEN).
 * This is a transient infrastructure error, not a business error.
 */
public class ServiceUnavailableException extends RuntimeException {
    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
