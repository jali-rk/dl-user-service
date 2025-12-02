package com.dopamine.userservice.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing a verification code for user verification.
 * Used for initial student registration and potentially email change flows.
 */
@Entity
@Table(name = "verification_codes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationCode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * The verification code. For REGISTRATION type, this is the same as the user's registration number.
     */
    @Column(name = "code", nullable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private VerificationType type;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /**
     * Number of failed verification attempts.
     * Maximum 3 attempts allowed.
     */
    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private int retryCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Timestamp when the code was successfully consumed.
     * If not null, the code has been used and cannot be reused.
     */
    @Column(name = "consumed_at")
    private Instant consumedAt;

    /**
     * Check if this verification code is expired.
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    /**
     * Check if this verification code has been consumed.
     */
    public boolean isConsumed() {
        return consumedAt != null;
    }

    /**
     * Check if this verification code has exceeded maximum retry attempts.
     */
    public boolean hasExceededRetries() {
        return retryCount >= 3;
    }

    /**
     * Check if this verification code is valid (not expired, not consumed, not exceeded retries).
     */
    public boolean isValid() {
        return !isExpired() && !isConsumed() && !hasExceededRetries();
    }

    /**
     * Increment the retry count.
     */
    public void incrementRetryCount() {
        this.retryCount++;
    }

    /**
     * Mark this verification code as consumed.
     */
    public void markAsConsumed() {
        this.consumedAt = Instant.now();
    }
}

