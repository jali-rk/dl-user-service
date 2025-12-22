package com.dopamine.userservice.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity to track the last issued sequential number for each code sub-pillar.
 * Ensures unique, non-repeating student codes with hierarchical structure.
 */
@Entity
@Table(name = "code_pillar_tracker")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodePillarTracker {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The base value for this sub-pillar (e.g., 560000).
     */
    @Column(name = "sub_pillar_base", nullable = false, unique = true)
    private Integer subPillarBase;

    /**
     * The last issued number from this sub-pillar (e.g., 560009).
     */
    @Column(name = "last_issued_number", nullable = false)
    private Integer lastIssuedNumber;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Check if this sub-pillar has reached its limit (9999 codes issued).
     */
    public boolean isAtLimit() {
        return lastIssuedNumber >= subPillarBase + 9999;
    }

    /**
     * Get the next available code number for this sub-pillar.
     */
    public Integer getNextCodeNumber() {
        if (isAtLimit()) {
            throw new IllegalStateException("Sub-pillar " + subPillarBase + " has reached its limit");
        }
        return lastIssuedNumber + 1;
    }
}

