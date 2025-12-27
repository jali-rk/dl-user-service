package com.dopamine.userservice.repository;

import com.dopamine.userservice.domain.EmailResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for EmailResetToken entity.
 */
@Repository
public interface EmailResetTokenRepository extends JpaRepository<EmailResetToken, UUID> {

    @Query("SELECT ert FROM EmailResetToken ert WHERE ert.userId = :userId AND ert.used = false")
    Optional<EmailResetToken> findActiveByUserId(@Param("userId") UUID userId);

    @Query("SELECT ert FROM EmailResetToken ert WHERE ert.tokenId = :tokenId AND ert.used = false AND ert.expiresAt > :now")
    Optional<EmailResetToken> findValidByTokenId(@Param("tokenId") UUID tokenId, @Param("now") Instant now);
}

