package com.example.userservice.auth;

import com.example.userservice.entity.DeviceSession;
import com.example.userservice.entity.User;
import com.example.userservice.repository.DeviceSessionRepository;
import com.example.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthSessionServiceTest {

    @Mock
    private JwtService jwtService;
    @Mock
    private DeviceSessionRepository deviceSessionRepository;
    @Mock
    private UserRepository userRepository;

    private AuthSessionService authSessionService;

    @BeforeEach
    void setUp() {
        authSessionService = new AuthSessionService(jwtService, deviceSessionRepository, userRepository);
    }

    @Test
    void createSession_revokesPreviousAndReturnsTokens() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(User.builder().id(1L).role("PAYMENT_USER").build()));
        when(jwtService.issueAccessToken(eq(1L), any())).thenReturn("access");
        when(jwtService.issueRefreshToken(1L)).thenReturn("refresh");
        when(jwtService.validateRefreshToken("refresh"))
                .thenReturn(new JwtService.JwtClaims(1L, List.of(), LocalDateTime.now().plusHours(2).atZone(java.time.ZoneId.systemDefault()).toInstant()));
        when(deviceSessionRepository.save(any(DeviceSession.class))).thenAnswer(i -> i.getArgument(0));

        AuthSessionService.TokenPair pair = authSessionService.createSession(1L, "device-1");

        verify(deviceSessionRepository).revokeAllByUserId(1L);
        assertEquals("access", pair.accessToken());
        assertEquals("refresh", pair.refreshToken());
        verify(deviceSessionRepository).save(any(DeviceSession.class));
    }

    @Test
    void validateRefreshTokenAndGetUserId_returnsNullWhenNoMatchingSession() {
        when(jwtService.validateRefreshToken("refresh"))
                .thenReturn(new JwtService.JwtClaims(1L, List.of(), LocalDateTime.now().plusHours(1).atZone(java.time.ZoneId.systemDefault()).toInstant()));
        when(deviceSessionRepository.findByUserIdAndIsRevokedFalse(1L)).thenReturn(List.of());

        Long userId = authSessionService.validateRefreshTokenAndGetUserId("refresh");

        assertNull(userId);
    }
}
