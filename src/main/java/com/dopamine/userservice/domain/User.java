package com.dopamine.userservice.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing a user in the system.
 * Can be a student, admin, or main admin.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "whatsapp_number")
    private String whatsappNumber;

    @Column(name = "school")
    private String school;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    /**
     * Registration number for students (null for admins).
     * This is the same value used as the initial verification code.
     */
    @Column(name = "code_number", unique = true)
    private String codeNumber;

    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private boolean isVerified = false;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    /**
     * Soft delete timestamp. If not null, the user is considered deleted.
     */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    /**
     * National Identity Card number (NIC). Required for students during registration.
     */
    @Column(name = "nic", unique = true)
    private String nic;

    /**
     * Check if this user is a student.
     */
    public boolean isStudent() {
        return role == Role.STUDENT;
    }

    /**
     * Check if this user is an admin (ADMIN or MAIN_ADMIN).
     */
    public boolean isAdmin() {
        return role == Role.ADMIN || role == Role.MAIN_ADMIN;
    }

    /**
     * Check if this user is a main admin.
     */
    public boolean isMainAdmin() {
        return role == Role.MAIN_ADMIN;
    }

    /**
     * Check if this user is soft deleted.
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Soft delete this user by setting deletedAt timestamp.
     */
    public void softDelete() {
        this.deletedAt = Instant.now();
    }

    /**
     * Mark this user as verified.
     */
    public void markAsVerified() {
        this.isVerified = true;
    }

    /**
     * Update last login timestamp.
     */
    public void updateLastLogin() {
        this.lastLoginAt = Instant.now();
    }
}
