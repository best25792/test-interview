package com.example.orderservice.exception;

public class ProductNotFoundException extends MerchantException {

    public ProductNotFoundException(Long id) {
        super("Product not found: " + id);
    }
}
