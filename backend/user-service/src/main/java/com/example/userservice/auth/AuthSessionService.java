package com.example.userservice.auth;

import com.example.userservice.entity.DeviceSession;
import com.example.userservice.repository.DeviceSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.List;

@Service
public class AuthSessionService {

    private final JwtService jwtService;
    private final DeviceSessionRepository deviceSessionRepository;

    public AuthSessionService(JwtService jwtService, DeviceSessionRepository deviceSessionRepository) {
        this.jwtService = jwtService;
        this.deviceSessionRepository = deviceSessionRepository;
    }

    /** SHA-256 hash for refresh token (JWT exceeds BCrypt's 72-byte limit). */
    private static String hashRefreshToken(String refreshToken) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(refreshToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /**
     * Single-device: revoke all existing sessions for the user, then create a new session.
     * Returns new access and refresh tokens.
     */
    @Transactional
    public TokenPair createSession(Long userId, String deviceId) {
        deviceSessionRepository.revokeAllByUserId(userId);
        String accessToken = jwtService.issueAccessToken(userId);
        String refreshToken = jwtService.issueRefreshToken(userId);
        JwtService.JwtClaims refreshClaims = jwtService.validateRefreshToken(refreshToken);
        String refreshTokenHash = hashRefreshToken(refreshToken);
        LocalDateTime expiresAt = LocalDateTime.ofInstant(refreshClaims.expiresAt(), ZoneId.systemDefault());
        DeviceSession session = DeviceSession.builder()
                .userId(userId)
                .deviceId(deviceId != null && !deviceId.isBlank() ? deviceId : "default")
                .refreshTokenHash(refreshTokenHash)
                .refreshTokenExpiresAt(expiresAt)
                .isRevoked(false)
                .createdAt(LocalDateTime.now())
                .build();
        deviceSessionRepository.save(session);
        return new TokenPair(accessToken, refreshToken);
    }

    /**
     * Validate refresh token and ensure it matches the stored session (single active session per user).
     * Returns userId if valid.
     */
    @Transactional(readOnly = true)
    public Long validateRefreshTokenAndGetUserId(String refreshToken) {
        JwtService.JwtClaims claims = jwtService.validateRefreshToken(refreshToken);
        Long userId = claims.userId();
        List<DeviceSession> sessions = deviceSessionRepository.findByUserIdAndIsRevokedFalse(userId);
        String incomingHash = hashRefreshToken(refreshToken);
        for (DeviceSession session : sessions) {
            if (incomingHash.equals(session.getRefreshTokenHash())) {
                if (session.getRefreshTokenExpiresAt().isBefore(LocalDateTime.now())) {
                    return null;
                }
                return userId;
            }
        }
        return null;
    }

    /**
     * Revoke the session for the given refresh token (logout).
     */
    @Transactional
    public void revokeSessionByRefreshToken(String refreshToken) {
        try {
            JwtService.JwtClaims claims = jwtService.validateRefreshToken(refreshToken);
            Long userId = claims.userId();
            List<DeviceSession> sessions = deviceSessionRepository.findByUserIdAndIsRevokedFalse(userId);
            String incomingHash = hashRefreshToken(refreshToken);
            for (DeviceSession session : sessions) {
                if (incomingHash.equals(session.getRefreshTokenHash())) {
                    session.setIsRevoked(true);
                    session.setRevokedAt(LocalDateTime.now());
                    deviceSessionRepository.save(session);
                    return;
                }
            }
        } catch (Exception ignored) {
            // invalid token - nothing to revoke
        }
    }

    public record TokenPair(String accessToken, String refreshToken) {}
}
