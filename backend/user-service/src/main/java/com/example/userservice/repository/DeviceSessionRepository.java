package com.example.userservice.repository;

import com.example.userservice.entity.DeviceSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DeviceSessionRepository extends JpaRepository<DeviceSession, Long> {

    Optional<DeviceSession> findByRefreshTokenHashAndIsRevokedFalse(String refreshTokenHash);

    List<DeviceSession> findByUserIdAndIsRevokedFalse(Long userId);

    @Modifying
    @Query("UPDATE DeviceSession d SET d.isRevoked = true, d.revokedAt = CURRENT_TIMESTAMP WHERE d.userId = :userId AND d.isRevoked = false")
    int revokeAllByUserId(@Param("userId") Long userId);
}
