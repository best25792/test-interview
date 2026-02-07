package com.example.orderservice.exception;

public class MerchantException extends RuntimeException {

    public MerchantException(String message) {
        super(message);
    }

    public MerchantException(String message, Throwable cause) {
        super(message, cause);
    }
}
