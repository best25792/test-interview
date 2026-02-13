package com.example.userservice.auth.otp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * OTP sender for prod: Cognito-compatible delivery (SMS/email via AWS).
 * Currently logs only; wire AWS SNS/SES or Cognito when app.auth.otp.sender=cognito.
 */
@Component
@ConditionalOnProperty(name = "app.auth.otp.sender", havingValue = "cognito")
public class AwsCognitoOtpSender implements OtpSender {

    private static final Logger log = LoggerFactory.getLogger(AwsCognitoOtpSender.class);

    @Override
    public void sendOtp(String channel, String destination, String code) {
        log.info("[OTP cognito] channel={}, destination={}, code=*** (would send via AWS)", channel, mask(destination));
        // TODO: integrate Cognito Identity Provider or SNS/SES to send OTP
    }

    private static String mask(String s) {
        if (s == null || s.length() < 4) return "****";
        return s.substring(0, 2) + "****" + s.substring(s.length() - 2);
    }
}
