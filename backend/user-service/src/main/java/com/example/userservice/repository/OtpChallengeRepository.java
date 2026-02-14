package com.example.userservice.repository;

import com.example.userservice.entity.OtpChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OtpChallengeRepository extends JpaRepository<OtpChallenge, Long> {

    @Query("SELECT o FROM OtpChallenge o WHERE o.userId = :userId AND o.channel = :channel AND o.isUsed = false AND o.expiresAt > :now ORDER BY o.createdAt DESC")
    List<OtpChallenge> findLatestActiveByUserIdAndChannel(
            @Param("userId") Long userId,
            @Param("channel") String channel,
            @Param("now") LocalDateTime now);

    default Optional<OtpChallenge> findLatestActiveChallenge(Long userId, String channel) {
        List<OtpChallenge> list = findLatestActiveByUserIdAndChannel(userId, channel, LocalDateTime.now());
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }
}
