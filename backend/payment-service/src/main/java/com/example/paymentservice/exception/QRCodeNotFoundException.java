package com.example.paymentservice.exception;

public class QRCodeNotFoundException extends RuntimeException {
    
    public QRCodeNotFoundException(String message) {
        super(message);
    }
    
    public QRCodeNotFoundException(String code, String reason) {
        super("QR code not found or invalid: " + code + ". Reason: " + reason);
    }
}
