package com.example.userservice.auth;

import com.example.userservice.config.OtpProperties;
import com.example.userservice.entity.OtpChallenge;
import com.example.userservice.entity.User;
import com.example.userservice.repository.OtpChallengeRepository;
import com.example.userservice.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class OtpService {

    private static final String CHANNEL_SMS = "SMS";
    private static final String CHANNEL_EMAIL = "EMAIL";

    private final OtpProperties properties;
    private final OtpChallengeRepository otpChallengeRepository;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    public OtpService(OtpProperties properties,
                      OtpChallengeRepository otpChallengeRepository,
                      UserRepository userRepository) {
        this.properties = properties;
        this.otpChallengeRepository = otpChallengeRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    /**
     * Generate a new OTP, store its hash, and return the raw code (to be sent via OtpSender).
     */
    @Transactional
    public String createChallenge(Long userId, String channel) {
        String code = generateOtp(properties.length());
        String codeHash = passwordEncoder.encode(code);
        LocalDateTime expiresAt = LocalDateTime.now().plus(properties.validity());
        OtpChallenge challenge = OtpChallenge.builder()
                .userId(userId)
                .channel(normalizeChannel(channel))
                .codeHash(codeHash)
                .expiresAt(expiresAt)
                .maxAttempts(properties.maxAttempts())
                .attempts(0)
                .isUsed(false)
                .createdAt(LocalDateTime.now())
                .build();
        otpChallengeRepository.save(challenge);
        return code;
    }

    /**
     * Verify OTP. For review accounts, accepts fixed OTP if configured.
     * @return true if verification succeeded
     */
    @Transactional
    public boolean verifyOtp(Long userId, String channel, String code) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return false;
        }
        User user = userOpt.get();
        if (user.getIsReviewAccount() && properties.isFixedOtpEnabled()
                && code != null && code.trim().equals(properties.reviewFixedOtp().trim())) {
            return true;
        }
        Optional<OtpChallenge> challengeOpt = otpChallengeRepository.findLatestActiveChallenge(userId, normalizeChannel(channel));
        if (challengeOpt.isEmpty()) {
            return false;
        }
        OtpChallenge challenge = challengeOpt.get();
        if (challenge.getAttempts() >= challenge.getMaxAttempts()) {
            return false;
        }
        if (challenge.getExpiresAt().isBefore(LocalDateTime.now())) {
            return false;
        }
        challenge.setAttempts(challenge.getAttempts() + 1);
        if (!passwordEncoder.matches(code, challenge.getCodeHash())) {
            otpChallengeRepository.save(challenge);
            return false;
        }
        challenge.setIsUsed(true);
        otpChallengeRepository.save(challenge);
        return true;
    }

    public static String channelSms() {
        return CHANNEL_SMS;
    }

    public static String channelEmail() {
        return CHANNEL_EMAIL;
    }

    private String generateOtp(int length) {
        int max = (int) Math.pow(10, length);
        int value = random.nextInt(max);
        return String.format("%0" + length + "d", value);
    }

    private String normalizeChannel(String channel) {
        if (channel == null) return CHANNEL_SMS;
        String c = channel.trim().toUpperCase();
        if ("EMAIL".equals(c)) return CHANNEL_EMAIL;
        return CHANNEL_SMS;
    }
}
