package com.example.orderservice.exception;

public class InsufficientStockException extends MerchantException {

    public InsufficientStockException(String productName, int requested, int available) {
        super(String.format("Insufficient stock for %s: requested %d, available %d",
                productName, requested, available));
    }
}
