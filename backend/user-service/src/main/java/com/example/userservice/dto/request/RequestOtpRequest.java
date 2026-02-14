package com.example.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RequestOtpRequest {

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[0-9]{10,20}$", message = "Invalid phone number")
    private String phoneNumber;

    /** SMS or EMAIL - where to send the OTP */
    private String channel = "SMS";
}
