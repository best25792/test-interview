package com.example.userservice.controller;

import com.example.userservice.auth.AuthService;
import com.example.userservice.dto.request.LogoutRequest;
import com.example.userservice.dto.request.RefreshTokenRequest;
import com.example.userservice.dto.request.RequestOtpRequest;
import com.example.userservice.dto.request.VerifyOtpRequest;
import com.example.userservice.dto.response.RequestOtpResponse;
import com.example.userservice.dto.response.TokenResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login/request-otp")
    public ResponseEntity<RequestOtpResponse> requestOtp(@Valid @RequestBody RequestOtpRequest request) {
        RequestOtpResponse response = authService.requestOtp(
                request.getPhoneNumber(),
                request.getChannel() != null ? request.getChannel() : "SMS"
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login/verify-otp")
    public ResponseEntity<TokenResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        TokenResponse response = authService.verifyOtp(
                request.getPhoneNumber(),
                request.getCode(),
                request.getDeviceId()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/token/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        TokenResponse response = authService.refresh(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/recovery/request-otp")
    public ResponseEntity<RequestOtpResponse> requestRecoveryOtp(@RequestParam String email) {
        RequestOtpResponse response = authService.requestRecoveryOtp(email);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/recovery/verify-otp")
    public ResponseEntity<TokenResponse> verifyRecoveryOtp(
            @RequestParam String email,
            @RequestParam String code,
            @RequestParam(required = false) String deviceId) {
        TokenResponse response = authService.verifyRecoveryOtp(email, code, deviceId);
        return ResponseEntity.ok(response);
    }
}
