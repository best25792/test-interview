package com.example.qrservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidateQRCodeRequest {
    
    @NotBlank(message = "QR code is required")
    private String code;
}
