package com.example.userservice.controller;

import com.example.userservice.auth.AuthService;
import com.example.userservice.config.JwtProperties;
import com.example.userservice.dto.response.RequestOtpResponse;
import com.example.userservice.dto.response.TokenResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Mock
    private AuthService authService;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties("user-service", "secret", Duration.ofMinutes(15), Duration.ofHours(2));
        AuthController controller = new AuthController(authService, jwtProperties);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void requestOtp_returns200() throws Exception {
        when(authService.requestOtp(eq("+1234567890"), anyString()))
                .thenReturn(RequestOtpResponse.builder().message("OTP sent").build());
        mockMvc.perform(post("/api/v1/auth/login/request-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"+1234567890\",\"channel\":\"SMS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("OTP sent"));
    }

    @Test
    void verifyOtp_returnsTokens() throws Exception {
        when(authService.verifyOtp(eq("+1234567890"), eq("123456"), any()))
                .thenReturn(TokenResponse.builder()
                        .accessToken("at")
                        .refreshToken("rt")
                        .expiresIn(900)
                        .build());
        mockMvc.perform(post("/api/v1/auth/login/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"+1234567890\",\"code\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("at"))
                .andExpect(jsonPath("$.refreshToken").value("rt"))
                .andExpect(jsonPath("$.expiresIn").value(900));
    }

    @Test
    void refresh_returnsTokens() throws Exception {
        when(authService.refresh("rt"))
                .thenReturn(TokenResponse.builder().accessToken("at2").refreshToken("rt2").expiresIn(900).build());
        mockMvc.perform(post("/api/v1/auth/token/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"rt\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("at2"));
    }

    @Test
    void logout_returns204() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"rt\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void jwtConfig_returnsIssuerAndAlgorithm() throws Exception {
        mockMvc.perform(get("/api/v1/auth/jwt-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issuer").exists())
                .andExpect(jsonPath("$.algorithm").value("HS256"));
    }
}
