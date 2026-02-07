package com.example.paymentservice.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateQRCodeRequest {
    
    @NotNull(message = "Payment ID is required")
    private Long paymentId;
    
    private String customerId; // Optional, for deactivating existing QR codes
}
