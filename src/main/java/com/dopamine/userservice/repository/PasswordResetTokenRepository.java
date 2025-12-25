package com.dopamine.userservice.repository;

import com.dopamine.userservice.domain.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for PasswordResetToken entity.
 */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    /**
     * Find a password reset token by token hash.
     */
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /**
     * Find a valid (not used, not expired) password reset token by token hash.
     */
    @Query("SELECT prt FROM PasswordResetToken prt WHERE prt.tokenHash = :tokenHash " +
            "AND prt.used = false AND prt.expiresAt > :now")
    Optional<PasswordResetToken> findValidByTokenHash(@Param("tokenHash") String tokenHash, @Param("now") Instant now);

    /**
     * Find the latest password reset token for a user.
     */
    @Query("SELECT prt FROM PasswordResetToken prt WHERE prt.userId = :userId ORDER BY prt.createdAt DESC LIMIT 1")
    Optional<PasswordResetToken> findLatestByUserId(@Param("userId") UUID userId);

    /**
     * Find a valid (not used, not expired) password reset token by tokenId.
     */
    @Query("SELECT prt FROM PasswordResetToken prt WHERE prt.tokenId = :tokenId " +
            "AND prt.used = false AND prt.expiresAt > :now")
    Optional<PasswordResetToken> findValidByTokenId(@Param("tokenId") UUID tokenId, @Param("now") Instant now);
}
