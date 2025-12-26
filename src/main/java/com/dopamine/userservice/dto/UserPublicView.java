package com.dopamine.userservice.dto;

import com.dopamine.userservice.domain.Role;
import com.dopamine.userservice.domain.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Public view of a user (safe to expose to external services).
 * Does not include sensitive information like password hash.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPublicView {
    private UUID id;
    private String fullName;
    private String email;
    private String whatsappNumber;
    private String school;
    private String address;
    private Role role;
    private UserStatus status;
    private String codeNumber;
    private boolean isVerified;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant lastLoginAt;
    private String nic;
}
