package com.example.userservice.auth;

import com.example.userservice.config.JwtProperties;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties(
                "test-issuer",
                "test-secret-at-least-256-bits-for-hs256-key-size",
                Duration.ofMinutes(15),
                Duration.ofHours(2)
        );
        jwtService = new JwtService(props);
    }

    @Test
    void issueAndValidateAccessToken_returnsUserIdAndRoles() {
        String token = jwtService.issueAccessToken(100L, List.of("PAYMENT_USER"));
        assertNotNull(token);
        JwtService.JwtClaims claims = jwtService.validateAccessToken(token);
        assertEquals(100L, claims.userId());
        assertEquals(List.of("PAYMENT_USER"), claims.roles());
        assertNotNull(claims.expiresAt());
    }

    @Test
    void validateAccessToken_rejectsRefreshToken() {
        String refreshToken = jwtService.issueRefreshToken(100L);
        assertThrows(JwtException.class, () -> jwtService.validateAccessToken(refreshToken));
    }

    @Test
    void issueAccessToken_withEmptyRoles() {
        String token = jwtService.issueAccessToken(200L, List.of());
        JwtService.JwtClaims claims = jwtService.validateAccessToken(token);
        assertEquals(200L, claims.userId());
        assertTrue(claims.roles().isEmpty());
    }

    @Test
    void issueAndValidateRefreshToken_returnsUserId() {
        String token = jwtService.issueRefreshToken(200L);
        assertNotNull(token);
        JwtService.JwtClaims claims = jwtService.validateRefreshToken(token);
        assertEquals(200L, claims.userId());
        assertNotNull(claims.roles());
    }

    @Test
    void validateRefreshToken_rejectsAccessToken() {
        String accessToken = jwtService.issueAccessToken(100L, List.of());
        assertThrows(JwtException.class, () -> jwtService.validateRefreshToken(accessToken));
    }

    @Test
    void validateAccessToken_throwsOnInvalidToken() {
        assertThrows(JwtException.class, () -> jwtService.validateAccessToken("invalid"));
    }
}
