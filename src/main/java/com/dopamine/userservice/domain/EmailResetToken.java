package com.dopamine.userservice.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing an email reset (verify new email) token.
 *
 * Used when a logged-in user requests to change their email.
 * Only a hash of the secret is stored; the raw token is returned once to BFF.
 */
@Entity
@Table(name = "email_reset_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "old_email", nullable = false)
    private String oldEmail;

    @Column(name = "new_email", nullable = false)
    private String newEmail;

    /**
     * Public identifier for this token (safe to embed in an email link).
     * Enables deterministic lookup without storing the raw secret.
     */
    @Column(name = "token_id", nullable = false, unique = true)
    private UUID tokenId;

    /**
     * Hash of the email reset secret.
     */
    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used", nullable = false)
    @Builder.Default
    private boolean used = false;

    @Column(name = "used_at")
    private Instant usedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !isExpired() && !used;
    }

    public void markAsUsed() {
        this.used = true;
        this.usedAt = Instant.now();
    }
}

