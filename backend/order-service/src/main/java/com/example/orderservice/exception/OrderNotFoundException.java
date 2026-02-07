package com.example.orderservice.exception;

public class OrderNotFoundException extends MerchantException {

    public OrderNotFoundException(Long id) {
        super("Order not found: " + id);
    }
}
