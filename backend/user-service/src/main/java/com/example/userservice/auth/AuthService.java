package com.example.userservice.auth;

import com.example.userservice.auth.otp.OtpSender;
import com.example.userservice.config.JwtProperties;
import com.example.userservice.dto.response.RequestOtpResponse;
import com.example.userservice.dto.response.TokenResponse;
import com.example.userservice.entity.User;
import com.example.userservice.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final OtpService otpService;
    private final OtpSender otpSender;
    private final AuthSessionService authSessionService;
    private final JwtProperties jwtProperties;

    public AuthService(UserRepository userRepository,
                       OtpService otpService,
                       OtpSender otpSender,
                       AuthSessionService authSessionService,
                       JwtProperties jwtProperties) {
        this.userRepository = userRepository;
        this.otpService = otpService;
        this.otpSender = otpSender;
        this.authSessionService = authSessionService;
        this.jwtProperties = jwtProperties;
    }

    @Transactional
    public RequestOtpResponse requestOtp(String phoneNumber, String channel) {
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is inactive");
        }
        String code = otpService.createChallenge(user.getId(), channel);
        String destination = "SMS".equalsIgnoreCase(channel) ? user.getPhoneNumber() : user.getEmail();
        if (destination == null) destination = user.getPhoneNumber();
        otpSender.sendOtp(channel, destination, code);
        return RequestOtpResponse.builder()
                .message("OTP sent")
                .build();
    }

    @Transactional
    public TokenResponse verifyOtp(String phoneNumber, String code, String deviceId) {
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (!otpService.verifyOtp(user.getId(), OtpService.channelSms(), code)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired OTP");
        }
        AuthSessionService.TokenPair pair = authSessionService.createSession(user.getId(), deviceId);
        long expiresIn = jwtProperties.accessTokenValidity().getSeconds();
        return TokenResponse.builder()
                .accessToken(pair.accessToken())
                .refreshToken(pair.refreshToken())
                .expiresIn(expiresIn)
                .build();
    }

    public TokenResponse refresh(String refreshToken) {
        Long userId = authSessionService.validateRefreshTokenAndGetUserId(refreshToken);
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token");
        }
        AuthSessionService.TokenPair pair = authSessionService.createSession(userId, "default");
        long expiresIn = jwtProperties.accessTokenValidity().getSeconds();
        return TokenResponse.builder()
                .accessToken(pair.accessToken())
                .refreshToken(pair.refreshToken())
                .expiresIn(expiresIn)
                .build();
    }

    @Transactional
    public void logout(String refreshToken) {
        authSessionService.revokeSessionByRefreshToken(refreshToken);
    }

    /** Recovery: send OTP to email. */
    @Transactional
    public RequestOtpResponse requestRecoveryOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is inactive");
        }
        String code = otpService.createChallenge(user.getId(), OtpService.channelEmail());
        otpSender.sendOtp(OtpService.channelEmail(), user.getEmail(), code);
        return RequestOtpResponse.builder()
                .message("Recovery OTP sent to email")
                .build();
    }

    /** Verify recovery OTP (e.g. to allow password reset or disable 2FA). */
    @Transactional
    public TokenResponse verifyRecoveryOtp(String email, String code, String deviceId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (!otpService.verifyOtp(user.getId(), OtpService.channelEmail(), code)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired recovery OTP");
        }
        AuthSessionService.TokenPair pair = authSessionService.createSession(user.getId(), deviceId);
        long expiresIn = jwtProperties.accessTokenValidity().getSeconds();
        return TokenResponse.builder()
                .accessToken(pair.accessToken())
                .refreshToken(pair.refreshToken())
                .expiresIn(expiresIn)
                .build();
    }
}
