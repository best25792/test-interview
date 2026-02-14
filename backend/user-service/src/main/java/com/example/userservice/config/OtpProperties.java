package com.example.userservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.auth.otp")
public record OtpProperties(
        int length,
        Duration validity,
        int maxAttempts,
        String reviewFixedOtp
) {
    public boolean isFixedOtpEnabled() {
        return reviewFixedOtp != null && !reviewFixedOtp.isBlank();
    }
}
