package com.example.userservice.auth.otp;

/**
 * Pluggable OTP delivery (SMS/email). No-op in non-prod; Cognito/SNS/SES in prod.
 */
public interface OtpSender {

    /**
     * Send OTP code to the given destination (phone number or email depending on channel).
     *
     * @param channel    SMS or EMAIL
     * @param destination phone number (e.g. +1234567890) or email address
     * @param code       the OTP code to send
     */
    void sendOtp(String channel, String destination, String code);
}
