package com.example.userservice.auth;

import com.example.userservice.config.OtpProperties;
import com.example.userservice.entity.OtpChallenge;
import com.example.userservice.entity.User;
import com.example.userservice.repository.OtpChallengeRepository;
import com.example.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock
    private OtpChallengeRepository otpChallengeRepository;
    @Mock
    private UserRepository userRepository;

    private OtpService otpService;
    private OtpProperties props;
    private User user;

    @BeforeEach
    void setUp() {
        props = new OtpProperties(6, Duration.ofMinutes(5), 5, "999999");
        otpService = new OtpService(props, otpChallengeRepository, userRepository);
        user = User.builder().id(1L).username("u").email("e@x.com").phoneNumber("+123").isReviewAccount(false).build();
    }

    @Test
    void createChallenge_savesChallengeAndReturnsCode() {
        when(otpChallengeRepository.save(any(OtpChallenge.class))).thenAnswer(i -> i.getArgument(0));
        String code = otpService.createChallenge(1L, "SMS");
        assertNotNull(code);
        assertEquals(6, code.length());
        assertTrue(code.matches("[0-9]+"));
        ArgumentCaptor<OtpChallenge> captor = ArgumentCaptor.forClass(OtpChallenge.class);
        verify(otpChallengeRepository).save(captor.capture());
        OtpChallenge saved = captor.getValue();
        assertEquals(1L, saved.getUserId());
        assertEquals("SMS", saved.getChannel());
        assertTrue(saved.getExpiresAt().isAfter(LocalDateTime.now()));
        assertEquals(5, saved.getMaxAttempts());
    }

    @Test
    void verifyOtp_success_whenCodeMatches() {
        String code = "123456";
        String codeHash = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(code);
        OtpChallenge challenge = OtpChallenge.builder()
                .id(1L).userId(1L).channel("SMS").codeHash(codeHash)
                .expiresAt(LocalDateTime.now().plusMinutes(5)).attempts(0).maxAttempts(5).isUsed(false)
                .createdAt(LocalDateTime.now()).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(otpChallengeRepository.findLatestActiveChallenge(1L, "SMS")).thenReturn(Optional.of(challenge));
        when(otpChallengeRepository.save(any(OtpChallenge.class))).thenAnswer(i -> i.getArgument(0));
        assertTrue(otpService.verifyOtp(1L, "SMS", code));
    }

    @Test
    void verifyOtp_fixedOtp_forReviewAccount() {
        user.setIsReviewAccount(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        assertTrue(otpService.verifyOtp(1L, "SMS", "999999"));
        verify(otpChallengeRepository, never()).findLatestActiveChallenge(any(), any());
    }

    @Test
    void verifyOtp_fails_whenUserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        assertFalse(otpService.verifyOtp(999L, "SMS", "123456"));
    }
}
