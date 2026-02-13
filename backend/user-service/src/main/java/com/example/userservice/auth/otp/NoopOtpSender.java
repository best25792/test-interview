package com.example.userservice.auth.otp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * No-op OTP sender for non-prod: logs only, no SMS/email sent.
 */
@Component
@ConditionalOnProperty(name = "app.auth.otp.sender", havingValue = "noop", matchIfMissing = true)
public class NoopOtpSender implements OtpSender {

    private static final Logger log = LoggerFactory.getLogger(NoopOtpSender.class);

    @Override
    public void sendOtp(String channel, String destination, String code) {
        log.info("[OTP noop] channel={}, destination={}, code={}", channel, mask(destination), code);
    }

    private static String mask(String s) {
        if (s == null || s.length() < 4) return "****";
        return s.substring(0, 2) + "****" + s.substring(s.length() - 2);
    }
}
