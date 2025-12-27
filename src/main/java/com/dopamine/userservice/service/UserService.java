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
     * Get paginated list of all active and verified students.
     * Returns students with status = ACTIVE and isVerified = true.
     *
     * @param page Page number (1-based)
     * @param pageSize Number of items per page (max 100)
     * @return Paginated response with students and total count
     */
    PaginatedStudentsResponse getStudentsPaginated(int page, int pageSize);

    /**
     * Get paginated list of students with optional filters.
     * Always returns users with role = STUDENT and not soft-deleted.
     */
    PaginatedStudentsResponse getStudentsPaginated(
            int page,
            int pageSize,
            String email,
            String name,
            String whatsappNumber,
            String codeNumber,
            Boolean isVerified,
            String status
    );

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

    /**
     * Soft delete an admin (ADMIN or MAIN_ADMIN) by ID.
     */
    void deleteAdmin(UUID adminId);

    /**
     * Request an email reset token for verifying the new email address.
     */
    EmailResetResponse requestEmailReset(EmailResetRequest request);

    /**
     * Confirm an email reset using the verification token.
     */
    EmailResetConfirmResponse confirmEmailReset(EmailResetConfirmRequest request);
}
