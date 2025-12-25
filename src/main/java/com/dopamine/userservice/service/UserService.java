package com.dopamine.userservice.service;

import com.dopamine.userservice.domain.Role;
import com.dopamine.userservice.domain.UserStatus;
import com.dopamine.userservice.dto.*;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for user-related operations.
 */
public interface UserService {

    /**
     * Register a new student.
     */
    StudentRegistrationResponse registerStudent(StudentRegistrationRequest request);

    /**
     * Verify a student's registration code.
     */
    UserPublicView verifyStudentCode(VerifyCodeRequest request);

    /**
     * Resend verification code to a student.
     */
    ResendVerificationCodeResponse resendVerificationCode(ResendVerificationCodeRequest request);

    /**
     * Get a student by ID.
     */
    UserPublicView getStudentById(UUID studentId);

    /**
     * Update a student's profile.
     */
    UserPublicView updateStudent(UUID studentId, StudentUpdateRequest request);

    /**
     * Create a new admin user.
     */
    UserPublicView createAdmin(AdminCreateRequest request);

    /**
     * Get an admin by ID.
     */
    UserPublicView getAdminById(UUID adminId);

    /**
     * Update an admin's profile.
     */
    UserPublicView updateAdmin(UUID adminId, AdminUpdateRequest request);

    /**
     * List all admins by role.
     */
    List<UserPublicView> listAdminsByRole(Role role);

    /**
     * List all admins by role and status.
     */
    List<UserPublicView> listAdminsByRoleAndStatus(Role role, UserStatus status);

    /**
     * Get a user by ID (any role).
     */
    UserPublicView getUserById(UUID userId);

    /**
     * Get a user by email (case-insensitive).
     */
    UserPublicView getUserByEmail(String email);

    /**
     * Validate user credentials for login.
     */
    CredentialsValidationResponse validateCredentials(CredentialsValidationRequest request);

    /**
     * Initiate password reset flow.
     */
    PasswordResetResponse requestPasswordReset(PasswordResetRequest request);

    /**
     * Confirm password reset with token.
     */
    void confirmPasswordReset(PasswordResetConfirmRequest request);

    /**
     * Get public batch data for multiple users by their IDs.
     * Returns only users that exist, are not deleted, have status = ACTIVE, and isVerified = true.
     * Returns minimal fields (id, fullName, whatsappNumber, email, codeNumber) as required by BFF.
     *
     * @param userIds list of user UUIDs (max 1000)
     * @return list of UserPublicBatchView objects for found users matching criteria
     */
    List<UserPublicBatchView> findUsersByIdsPublicData(List<UUID> userIds);
}

