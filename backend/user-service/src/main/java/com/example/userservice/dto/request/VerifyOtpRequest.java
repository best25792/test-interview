package com.example.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class VerifyOtpRequest {

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[0-9]{10,20}$", message = "Invalid phone number")
    private String phoneNumber;

    @NotBlank(message = "OTP code is required")
    @Pattern(regexp = "^[0-9]{4,8}$", message = "Invalid OTP format")
    private String code;

    /** Device identifier for single-device session (optional) */
    private String deviceId;
}
