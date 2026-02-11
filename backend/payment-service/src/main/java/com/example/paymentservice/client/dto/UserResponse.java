package com.example.paymentservice.client.dto;

public record UserResponse(Long id, String username, String email, Boolean isActive, Boolean isVerified) {}
