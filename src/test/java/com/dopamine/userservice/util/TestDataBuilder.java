package com.dopamine.userservice.util;

import com.dopamine.userservice.domain.*;
import com.dopamine.userservice.dto.*;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Test data builder utility for creating test objects.
 * Provides factory methods for creating domain entities and DTOs.
 */
public class TestDataBuilder {

    // User Builders
    public static User.UserBuilder defaultStudent() {
        return User.builder()
                .id(UUID.randomUUID())
                .fullName("John Doe")
                .email("john.doe@example.com")
                .whatsappNumber("+94771234567")
                .school("Royal College")
                .address("123 Main St, Colombo")
                .role(Role.STUDENT)
                .status(UserStatus.ACTIVE)
                .codeNumber("1001")
                .isVerified(false)
                .passwordHash("$2a$10$hashedPassword")
                .createdAt(Instant.now())
                .updatedAt(Instant.now());
    }

    public static User.UserBuilder verifiedStudent() {
        return defaultStudent().isVerified(true);
    }

    public static User.UserBuilder defaultAdmin() {
        return User.builder()
                .id(UUID.randomUUID())
                .fullName("Admin User")
                .email("admin@example.com")
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .isVerified(true)
                .passwordHash("$2a$10$hashedPassword")
                .createdAt(Instant.now())
                .updatedAt(Instant.now());
    }

    public static User.UserBuilder defaultMainAdmin() {
        return User.builder()
                .id(UUID.randomUUID())
                .fullName("Main Admin")
                .email("mainadmin@example.com")
                .role(Role.MAIN_ADMIN)
                .status(UserStatus.ACTIVE)
                .isVerified(true)
                .passwordHash("$2a$10$hashedPassword")
                .createdAt(Instant.now())
                .updatedAt(Instant.now());
    }

    // DTO Builders
    public static StudentRegistrationRequest.StudentRegistrationRequestBuilder defaultStudentRegistrationRequest() {
        return StudentRegistrationRequest.builder()
                .fullName("Jane Smith")
                .email("jane.smith@example.com")
                .whatsappNumber("+94771234568")
                .school("Royal College")
                .address("456 Side St, Colombo")
                .password("SecurePassword123");
    }

    public static AdminCreateRequest.AdminCreateRequestBuilder defaultAdminCreateRequest() {
        return AdminCreateRequest.builder()
                .fullName("New Admin")
                .email("newadmin@example.com")
                .password("AdminPassword123")
                .role(Role.ADMIN);
    }

    public static VerifyCodeRequest.VerifyCodeRequestBuilder defaultVerifyCodeRequest() {
        return VerifyCodeRequest.builder()
                .email("john.doe@example.com")
                .code("1001");
    }

    public static CredentialsValidationRequest.CredentialsValidationRequestBuilder defaultCredentialsRequest() {
        return CredentialsValidationRequest.builder()
                .email("john.doe@example.com")
                .password("SecurePassword123");
    }

    public static StudentUpdateRequest.StudentUpdateRequestBuilder defaultStudentUpdateRequest() {
        return StudentUpdateRequest.builder()
                .fullName("Updated Name")
                .whatsappNumber("+94771234569")
                .school("New School")
                .address("New Address");
    }

    public static AdminUpdateRequest.AdminUpdateRequestBuilder defaultAdminUpdateRequest() {
        return AdminUpdateRequest.builder()
                .fullName("Updated Admin Name");
    }

    public static PasswordResetRequest.PasswordResetRequestBuilder defaultPasswordResetRequest() {
        return PasswordResetRequest.builder()
                .email("john.doe@example.com");
    }

    public static PasswordResetConfirmRequest.PasswordResetConfirmRequestBuilder defaultPasswordResetConfirmRequest() {
        return PasswordResetConfirmRequest.builder()
                .token("test-token-uuid")
                .newPassword("NewSecurePassword123");
    }

    // Domain Entity Builders
    public static VerificationCode.VerificationCodeBuilder defaultVerificationCode(UUID userId) {
        return VerificationCode.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .code("1001")
                .type(VerificationType.REGISTRATION)
                .expiresAt(Instant.now().plus(Duration.ofMinutes(2)))
                .retryCount(0)
                .createdAt(Instant.now());
    }

    public static VerificationCode.VerificationCodeBuilder expiredVerificationCode(UUID userId) {
        return defaultVerificationCode(userId)
                .expiresAt(Instant.now().minus(Duration.ofMinutes(5)));
    }

    public static VerificationCode.VerificationCodeBuilder consumedVerificationCode(UUID userId) {
        return defaultVerificationCode(userId)
                .consumedAt(Instant.now());
    }

    public static PasswordResetToken.PasswordResetTokenBuilder defaultPasswordResetToken(UUID userId, String tokenHash) {
        return PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plus(Duration.ofMinutes(30)))
                .used(false)
                .createdAt(Instant.now());
    }

    public static PasswordResetToken.PasswordResetTokenBuilder expiredPasswordResetToken(UUID userId, String tokenHash) {
        return defaultPasswordResetToken(userId, tokenHash)
                .expiresAt(Instant.now().minus(Duration.ofMinutes(5)));
    }

    public static PasswordResetToken.PasswordResetTokenBuilder usedPasswordResetToken(UUID userId, String tokenHash) {
        return defaultPasswordResetToken(userId, tokenHash)
                .used(true)
                .usedAt(Instant.now());
    }
}

