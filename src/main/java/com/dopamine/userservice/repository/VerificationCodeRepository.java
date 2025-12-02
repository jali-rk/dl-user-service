package com.dopamine.userservice.repository;

import com.dopamine.userservice.domain.VerificationCode;
import com.dopamine.userservice.domain.VerificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for VerificationCode entity.
 */
@Repository
public interface VerificationCodeRepository extends JpaRepository<VerificationCode, UUID> {

    /**
     * Find the latest verification code for a user by type.
     */
    @Query("SELECT vc FROM VerificationCode vc WHERE vc.userId = :userId AND vc.type = :type ORDER BY vc.createdAt DESC LIMIT 1")
    Optional<VerificationCode> findLatestByUserIdAndType(@Param("userId") UUID userId, @Param("type") VerificationType type);

    /**
     * Find the latest active (not consumed, not expired) verification code for a user by type.
     */
    @Query("SELECT vc FROM VerificationCode vc WHERE vc.userId = :userId AND vc.type = :type " +
            "AND vc.consumedAt IS NULL AND vc.expiresAt > :now ORDER BY vc.createdAt DESC LIMIT 1")
    Optional<VerificationCode> findLatestActiveByUserIdAndType(
            @Param("userId") UUID userId,
            @Param("type") VerificationType type,
            @Param("now") Instant now
    );
}

