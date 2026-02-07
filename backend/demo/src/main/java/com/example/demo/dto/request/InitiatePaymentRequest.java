package com.example.demo.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InitiatePaymentRequest {
    
    @NotNull(message = "User ID is required")
    private Long userId;
}
