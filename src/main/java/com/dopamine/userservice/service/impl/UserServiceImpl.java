package com.dopamine.userservice.service.impl;

import com.dopamine.userservice.constants.ApplicationConstants;
import com.dopamine.userservice.domain.*;
import com.dopamine.userservice.dto.*;
import com.dopamine.userservice.exception.*;
import com.dopamine.userservice.mapper.UserMapper;
import com.dopamine.userservice.repository.EmailResetTokenRepository;
import com.dopamine.userservice.repository.PasswordResetTokenRepository;
import com.dopamine.userservice.repository.UserRepository;
import com.dopamine.userservice.repository.VerificationCodeRepository;
import com.dopamine.userservice.service.EmailNotificationService;
import com.dopamine.userservice.service.StudentCodeGeneratorService;
import com.dopamine.userservice.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of UserService.
 */
@Service
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final VerificationCodeRepository verificationCodeRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailResetTokenRepository emailResetTokenRepository;
    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final StudentCodeGeneratorService studentCodeGeneratorService;
    private final EmailNotificationService emailNotificationService;

    public UserServiceImpl(
            UserRepository userRepository,
            VerificationCodeRepository verificationCodeRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            EmailResetTokenRepository emailResetTokenRepository,
            UserMapper userMapper,
            BCryptPasswordEncoder passwordEncoder,
            StudentCodeGeneratorService studentCodeGeneratorService,
            EmailNotificationService emailNotificationService
    ) {
        this.userRepository = userRepository;
        this.verificationCodeRepository = verificationCodeRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailResetTokenRepository = emailResetTokenRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.studentCodeGeneratorService = studentCodeGeneratorService;
        this.emailNotificationService = emailNotificationService;
    }

    @Override
    @Transactional
    public StudentRegistrationResponse registerStudent(StudentRegistrationRequest request) {
        log.info("Registering new student with email: {}", request.getEmail());

        // Check if user already exists
        if (userRepository.existsByEmailIgnoreCaseAndNotDeleted(request.getEmail())) {
            log.warn("User with email {} already exists", request.getEmail());
            throw new UserAlreadyExistsException("User with email " + request.getEmail() + " already exists");
        }

        // Generate registration number from sequence
        String registrationNumber = studentCodeGeneratorService.generateStudentCode();
        log.debug("Generated registration number: {}", registrationNumber);

        // Create user entity
        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail().toLowerCase())
                .whatsappNumber(request.getWhatsappNumber())
                .school(request.getSchool())
                .address(request.getAddress())
                .nic(request.getNic())
                .role(Role.STUDENT)
                .status(UserStatus.ACTIVE)
                .codeNumber(registrationNumber)
                .isVerified(false)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();

        // Save user
        user = userRepository.save(user);
        log.info("Created student user with ID: {} and registration number: {}", user.getId(), registrationNumber);

        // Create verification code (same as registration number)
        VerificationCode verificationCode = VerificationCode.builder()
                .userId(user.getId())
                .code(registrationNumber)
                .type(VerificationType.REGISTRATION)
                .expiresAt(Instant.now().plus(ApplicationConstants.VerificationCode.REGISTRATION_CODE_EXPIRY))
                .retryCount(0)
                .build();

        verificationCodeRepository.save(verificationCode);
        log.info("Created verification code for user: {}", user.getId());

        // Send verification code email via BFF AFTER transaction commit
        UUID userId = user.getId();
        String codeToSend = registrationNumber;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                emailNotificationService.sendVerificationCodeEmail(userId, codeToSend);
            }
        });

        return StudentRegistrationResponse.builder()
                .user(userMapper.toPublicView(user))
                .verificationCodeGenerated(true)
                .build();
    }

    @Override
    @Transactional
    public UserPublicView verifyStudentCode(VerifyCodeRequest request) {
        log.info("Verifying student code for email: {}", request.getEmail());

        // Find user by email: What if two students are gonna use the same email? -> Handled by unique constraint on registration
        User user = userRepository.findByEmailIgnoreCaseAndNotDeleted(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + request.getEmail()));

        if (user.isVerified()) {
            log.info("User {} is already verified", user.getId());
            return userMapper.toPublicView(user);
        }

        // Find latest active verification code
        VerificationCode verificationCode = verificationCodeRepository
                .findLatestActiveByUserIdAndType(user.getId(), VerificationType.REGISTRATION, Instant.now())
                .orElseThrow(() -> new InvalidVerificationCodeException("No active verification code found"));

        // Check retry limit
        if (verificationCode.hasExceededRetries()) {
            log.warn("Verification code for user {} has exceeded retry limit", user.getId());
            throw new InvalidVerificationCodeException("Verification code has exceeded maximum retry attempts");
        }

        // Validate code
        if (!verificationCode.getCode().equals(request.getCode())) {
            log.warn("Invalid verification code attempt for user {}", user.getId());
            verificationCode.incrementRetryCount();

            // If this was the 3rd failed attempt, mark as consumed
            if (verificationCode.hasExceededRetries()) {
                verificationCode.markAsConsumed();
                log.info("Verification code for user {} marked as consumed after 3 failed attempts", user.getId());
            }

            verificationCodeRepository.save(verificationCode);
            throw new InvalidVerificationCodeException("Invalid verification code");
        }

        // Code is valid - mark user as verified and code as consumed
        verificationCode.markAsConsumed();
        verificationCodeRepository.save(verificationCode);

        user.setVerified(true);
        user = userRepository.save(user);

        log.info("Successfully verified user: {}", user.getId());
        return userMapper.toPublicView(user);
    }

    @Override
    @Transactional
    public ResendVerificationCodeResponse resendVerificationCode(ResendVerificationCodeRequest request) {
        log.info("Resending verification code for email: {}", request.getEmail());

        // Find user by email
        User user = userRepository.findByEmailIgnoreCaseAndNotDeleted(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + request.getEmail()));

        // Check if user is already verified
        if (user.isVerified()) {
            log.info("User {} is already verified, no need to resend code", user.getId());
            return new ResendVerificationCodeResponse(false, "User is already verified", null);
        }

        // Check if user is a student
        if (user.getRole() != Role.STUDENT) {
            log.warn("Attempted to resend verification code for non-student user: {}", user.getId());
            throw new IllegalArgumentException("Verification codes can only be resent for students");
        }

        // Mark any existing active codes as consumed (invalidate old codes)
        verificationCodeRepository.findLatestActiveByUserIdAndType(
                user.getId(),
                VerificationType.REGISTRATION,
                Instant.now()
        ).ifPresent(oldCode -> {
            oldCode.markAsConsumed();
            verificationCodeRepository.save(oldCode);
            log.debug("Marked old verification code as consumed for user: {}", user.getId());
        });

        // Generate a NEW random student code (this will be the new permanent student number)
        String newStudentCode = studentCodeGeneratorService.generateStudentCode();
        log.debug("Generated new student code: {}", newStudentCode);

        // Update user's permanent code number with the new code
        user.setCodeNumber(newStudentCode);
        userRepository.save(user);
        log.info("Updated user {} with new code number: {}", user.getId(), newStudentCode);

        // Create new verification code with the same code (verification code = student code)
        VerificationCode verificationCodeEntity = VerificationCode.builder()
                .userId(user.getId())
                .code(newStudentCode) // Same as the new student code number
                .type(VerificationType.REGISTRATION)
                .expiresAt(Instant.now().plus(ApplicationConstants.VerificationCode.REGISTRATION_CODE_EXPIRY))
                .retryCount(0)
                .build();

        verificationCodeRepository.save(verificationCodeEntity);
        log.info("Created new verification code for user: {}", user.getId());

        // Send resend verification code email via BFF AFTER transaction commit
        UUID userId = user.getId();
        String codeToSend = newStudentCode;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                emailNotificationService.sendResendVerificationCodeEmail(userId, codeToSend);
            }
        });

        return new ResendVerificationCodeResponse(true, "Verification code has been resent", newStudentCode);
    }

    @Override
    @Transactional(readOnly = true)
    public UserPublicView getStudentById(UUID studentId) {
        log.debug("Getting student by ID: {}", studentId);
        User user = userRepository.findByIdAndNotDeleted(studentId)
                .orElseThrow(() -> new UserNotFoundException("Student not found with ID: " + studentId));

        if (user.getRole() != Role.STUDENT) {
            throw new UserNotFoundException("User with ID " + studentId + " is not a student");
        }

        return userMapper.toPublicView(user);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedStudentsResponse getStudentsPaginated(int page, int pageSize) {
        // Backwards-compatible behavior: active + verified students only
        return getStudentsPaginated(page, pageSize, null, null, null, null, true, UserStatus.ACTIVE.name());
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedStudentsResponse getStudentsPaginated(
            int page,
            int pageSize,
            String email,
            String name,
            String whatsappNumber,
            String codeNumber,
            Boolean isVerified,
            String status
    ) {
        log.info("Searching students: page={}, pageSize={}, email={}, name={}, whatsappNumber={}, codeNumber={}, isVerified={}, status={}",
                page, pageSize, email, name, whatsappNumber, codeNumber, isVerified, status);

        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        UserStatus parsedStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                parsedStatus = UserStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // Invalid status value - ignore filter
                log.warn("Ignoring invalid status filter: {}", status);
            }
        }

        Page<User> studentPage = userRepository.searchStudents(
                email,
                name,
                whatsappNumber,
                codeNumber,
                isVerified,
                parsedStatus,
                pageable
        );

        List<StudentListItem> items = studentPage.getContent().stream()
                .map(userMapper::toStudentListItem)
                .collect(Collectors.toList());

        return PaginatedStudentsResponse.builder()
                .items(items)
                .total(studentPage.getTotalElements())
                .build();
    }

    @Override
    @Transactional
    public UserPublicView updateStudent(UUID studentId, StudentUpdateRequest request) {
        log.info("Updating student: {}", studentId);
        User user = userRepository.findByIdAndNotDeleted(studentId)
                .orElseThrow(() -> new UserNotFoundException("Student not found with ID: " + studentId));

        if (user.getRole() != Role.STUDENT) {
            throw new UserNotFoundException("User with ID " + studentId + " is not a student");
        }

        // Update only provided fields
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getWhatsappNumber() != null) {
            user.setWhatsappNumber(request.getWhatsappNumber());
        }
        if (request.getSchool() != null) {
            user.setSchool(request.getSchool());
        }
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress());
        }

        user = userRepository.save(user);
        log.info("Updated student: {}", studentId);
        return userMapper.toPublicView(user);
    }

    @Override
    @Transactional
    public UserPublicView createAdmin(AdminCreateRequest request) {
        log.info("Creating admin with email: {} and role: {}", request.getEmail(), request.getRole());

        // Validate role (should be ADMIN or MAIN_ADMIN)
        if (request.getRole() != Role.ADMIN && request.getRole() != Role.MAIN_ADMIN) {
            throw new IllegalArgumentException("Invalid role for admin creation: " + request.getRole());
        }

        // Check if user already exists
        if (userRepository.existsByEmailIgnoreCaseAndNotDeleted(request.getEmail())) {
            log.warn("User with email {} already exists", request.getEmail());
            throw new UserAlreadyExistsException("User with email " + request.getEmail() + " already exists");
        }

        // Create admin user
        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail().toLowerCase())
                .role(request.getRole())
                .status(UserStatus.ACTIVE)
                .isVerified(true) // Admins are automatically verified
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();

        user = userRepository.save(user);
        log.info("Created admin user with ID: {} and role: {}", user.getId(), user.getRole());

        return userMapper.toPublicView(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserPublicView getAdminById(UUID adminId) {
        log.debug("Getting admin by ID: {}", adminId);
        User user = userRepository.findByIdAndNotDeleted(adminId)
                .orElseThrow(() -> new UserNotFoundException("Admin not found with ID: " + adminId));

        if (!user.isAdmin()) {
            throw new UserNotFoundException("User with ID " + adminId + " is not an admin");
        }

        return userMapper.toPublicView(user);
    }

    @Override
    @Transactional
    public UserPublicView updateAdmin(UUID adminId, AdminUpdateRequest request) {
        log.info("Updating admin: {}", adminId);
        User user = userRepository.findByIdAndNotDeleted(adminId)
                .orElseThrow(() -> new UserNotFoundException("Admin not found with ID: " + adminId));

        if (!user.isAdmin()) {
            throw new UserNotFoundException("User with ID " + adminId + " is not an admin");
        }

        // Update only provided fields
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }

        user = userRepository.save(user);
        log.info("Updated admin: {}", adminId);
        return userMapper.toPublicView(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserPublicView> listAdminsByRole(Role role) {
        log.debug("Listing admins by role: {}", role);
        if (role != Role.ADMIN && role != Role.MAIN_ADMIN) {
            throw new IllegalArgumentException("Invalid admin role: " + role);
        }

        List<User> admins = userRepository.findByRoleAndNotDeleted(role);
        return admins.stream()
                .map(userMapper::toPublicView)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserPublicView> listAdminsByRoleAndStatus(Role role, UserStatus status) {
        log.debug("Listing admins by role: {} and status: {}", role, status);
        if (role != Role.ADMIN && role != Role.MAIN_ADMIN) {
            throw new IllegalArgumentException("Invalid admin role: " + role);
        }

        List<User> admins = userRepository.findByRoleAndStatusAndNotDeleted(role, status);
        return admins.stream()
                .map(userMapper::toPublicView)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UserPublicView getUserById(UUID userId) {
        log.debug("Getting user by ID: {}", userId);
        User user = userRepository.findByIdAndNotDeleted(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
        return userMapper.toPublicView(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserPublicView getUserByEmail(String email) {
        log.debug("Getting user by email: {}", email);
        User user = userRepository.findByEmailIgnoreCaseAndNotDeleted(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
        return userMapper.toPublicView(user);
    }

    @Override
    @Transactional
    public CredentialsValidationResponse validateCredentials(CredentialsValidationRequest request) {
        log.info("Validating credentials for email: {}", request.getEmail());

        try {
            // Find user by email
            User user = userRepository.findByEmailIgnoreCaseAndNotDeleted(request.getEmail())
                    .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

            // Verify password
            if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
                log.warn("Invalid password attempt for user: {}", user.getId());
                throw new InvalidCredentialsException("Invalid credentials");
            }

            // Check user status
            if (user.getStatus() != UserStatus.ACTIVE) {
                log.warn("User {} is not active (status: {})", user.getId(), user.getStatus());
                throw new InvalidCredentialsException("User account is not active");
            }

            // For students, check if verified
            if (user.getRole() == Role.STUDENT && !user.isVerified()) {
                log.warn("Student {} is not verified", user.getId());
                throw new UnverifiedUserException("Student account is not verified");
            }

            // Update last login timestamp
            user.setLastLoginAt(Instant.now());
            userRepository.save(user);

            log.info("Successfully validated credentials for user: {}", user.getId());
            return CredentialsValidationResponse.builder()
                    .valid(true)
                    .user(userMapper.toPublicView(user))
                    .build();

        } catch (InvalidCredentialsException | UnverifiedUserException e) {
            // Re-throw these exceptions to be handled by the controller advice
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during credential validation", e);
            throw new InvalidCredentialsException("Invalid credentials");
        }
    }

    @Override
    @Transactional
    public PasswordResetResponse requestPasswordReset(PasswordResetRequest request) {
        log.info("Password reset requested for email: {}", request.getEmail());

        // Always return success to avoid leaking user existence
        // But only create token if user exists
        try {
            User user = userRepository.findByEmailIgnoreCaseAndNotDeleted(request.getEmail())
                    .orElse(null);

            if (user != null) {
                // Generate tokenId (lookup key) + strong random secret (sent to user)
                UUID tokenId = UUID.randomUUID();

                // BCrypt can only accept up to 72 bytes, so keep the secret short.
                // 32 random bytes -> ~43 chars base64url (well under 72)
                byte[] secretBytes = new byte[32];
                new SecureRandom().nextBytes(secretBytes);
                String secret = Base64.getUrlEncoder().withoutPadding().encodeToString(secretBytes);

                // Store only a hash of the secret, never the secret itself
                String secretHash = passwordEncoder.encode(secret);

                PasswordResetToken token = PasswordResetToken.builder()
                        .userId(user.getId())
                        .tokenId(tokenId)
                        .tokenHash(secretHash)
                        .expiresAt(Instant.now().plus(ApplicationConstants.PasswordReset.PASSWORD_RESET_TOKEN_EXPIRY))
                        .used(false)
                        .build();

                passwordResetTokenRepository.save(token);
                log.info("Created password reset token for user: {}", user.getId());

                // Combined token is convenient for email links: {tokenId}.{secret}
                // Note: BFF is responsible for sending the password reset email.
                String combinedToken = tokenId + "." + secret;

                return PasswordResetResponse.builder()
                        .message("Password reset instructions have been sent to your email")
                        .token(combinedToken)
                        .build();
            } else {
                log.info("Password reset requested for non-existent email: {}", request.getEmail());
            }

        } catch (Exception e) {
            log.error("Error during password reset request", e);
        }

        // Always return generic success message
        return PasswordResetResponse.builder()
                .message("If the email exists, password reset instructions have been sent")
                .build();
    }

    @Override
    @Transactional
    public void confirmPasswordReset(PasswordResetConfirmRequest request) {
        log.info("Confirming password reset with token");

        // Expected token format: {tokenId}.{secret}
        String token = request.getToken();
        String[] parts = token != null ? token.split("\\.", 2) : new String[0];
        if (parts.length != 2) {
            throw new InvalidPasswordResetTokenException("Invalid or expired password reset token");
        }

        UUID tokenId;
        try {
            tokenId = UUID.fromString(parts[0]);
        } catch (IllegalArgumentException e) {
            throw new InvalidPasswordResetTokenException("Invalid or expired password reset token");
        }

        String secret = parts[1];

        // Lookup token deterministically by tokenId (cannot reliably lookup by BCrypt hash)
        PasswordResetToken tokenEntity = passwordResetTokenRepository
                .findValidByTokenId(tokenId, Instant.now())
                .orElseThrow(() -> new InvalidPasswordResetTokenException("Invalid or expired password reset token"));

        // Verify secret matches stored hash
        if (!passwordEncoder.matches(secret, tokenEntity.getTokenHash())) {
            log.warn("Invalid password reset token secret attempt");
            throw new InvalidPasswordResetTokenException("Invalid or expired password reset token");
        }

        // Find user
        User user = userRepository.findByIdAndNotDeleted(tokenEntity.getUserId())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // Update password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Mark token as used
        tokenEntity.markAsUsed();
        passwordResetTokenRepository.save(tokenEntity);

        log.info("Successfully reset password for user: {}", user.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserPublicBatchView> findUsersByIdsPublicData(List<UUID> userIds) {
        log.debug("Fetching public batch data for {} users", userIds.size());

        if (userIds.isEmpty()) {
            log.debug("Empty user IDs list provided, returning empty result");
            return List.of();
        }

        // Fetch users in bulk using single query with WHERE IN clause
        // Repository query automatically filters: status = ACTIVE AND isVerified = true
        List<User> users = userRepository.findByIdsAndNotDeleted(userIds);

        log.info("Found {} ACTIVE and verified users out of {} requested IDs", users.size(), userIds.size());

        // Map to minimal batch view DTOs (only id, fullName, whatsappNumber, email, codeNumber)
        return users.stream()
                .map(userMapper::toPublicBatchView)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteAdmin(UUID adminId) {
        log.info("Deleting admin: {}", adminId);

        User user = userRepository.findByIdAndNotDeleted(adminId)
                .orElseThrow(() -> new UserNotFoundException("Admin not found with ID: " + adminId));

        if (!user.isAdmin()) {
            throw new UserNotFoundException("Admin not found with ID: " + adminId);
        }

        user.softDelete();
        userRepository.save(user);
        log.info("Soft deleted admin: {}", adminId);
    }

    @Override
    @Transactional
    public EmailResetResponse requestEmailReset(EmailResetRequest request) {
        log.info("Email reset requested for userId={} oldEmail={} newEmail={}", request.getUserId(), request.getOldEmail(), request.getNewEmail());

        User user = userRepository.findByIdAndNotDeleted(request.getUserId())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (user.getEmail() == null || request.getOldEmail() == null || !user.getEmail().equalsIgnoreCase(request.getOldEmail())) {
            throw new UserNotFoundException("User not found");
        }

        // Ensure newEmail is not used by another user (case-insensitive)
        var existing = userRepository.findByEmailIgnoreCaseAndNotDeleted(request.getNewEmail());
        if (existing.isPresent() && !existing.get().getId().equals(user.getId())) {
            throw new EmailAlreadyInUseException("Email already in use");
        }

        // Invalidate any existing active token for this user
        emailResetTokenRepository.findActiveByUserId(user.getId()).ifPresent(active -> {
            active.markAsUsed();
            emailResetTokenRepository.save(active);
        });

        UUID tokenId = UUID.randomUUID();

        byte[] secretBytes = new byte[32];
        new SecureRandom().nextBytes(secretBytes);
        String secret = Base64.getUrlEncoder().withoutPadding().encodeToString(secretBytes);

        String secretHash = passwordEncoder.encode(secret);

        EmailResetToken tokenEntity = EmailResetToken.builder()
                .userId(user.getId())
                .oldEmail(request.getOldEmail())
                .newEmail(request.getNewEmail())
                .tokenId(tokenId)
                .tokenHash(secretHash)
                .expiresAt(Instant.now().plus(ApplicationConstants.EmailReset.EMAIL_RESET_TOKEN_EXPIRY))
                .used(false)
                .build();

        emailResetTokenRepository.save(tokenEntity);

        // Token format consistent with password reset: {tokenId}.{secret}
        String combinedToken = tokenId + "." + secret;

        return EmailResetResponse.builder()
                .token(combinedToken)
                .build();
    }

    @Override
    @Transactional
    public EmailResetConfirmResponse confirmEmailReset(EmailResetConfirmRequest request) {
        log.info("Confirming email reset with token");

        String token = request.getToken();
        String[] parts = token != null ? token.split("\\.", 2) : new String[0];
        if (parts.length != 2) {
            throw new InvalidEmailResetTokenException("Invalid or expired email reset token");
        }

        UUID tokenId;
        try {
            tokenId = UUID.fromString(parts[0]);
        } catch (IllegalArgumentException e) {
            throw new InvalidEmailResetTokenException("Invalid or expired email reset token");
        }

        String secret = parts[1];

        EmailResetToken tokenEntity = emailResetTokenRepository
                .findValidByTokenId(tokenId, Instant.now())
                .orElseThrow(() -> new InvalidEmailResetTokenException("Invalid or expired email reset token"));

        if (!passwordEncoder.matches(secret, tokenEntity.getTokenHash())) {
            log.warn("Invalid email reset token secret attempt");
            throw new InvalidEmailResetTokenException("Invalid or expired email reset token");
        }

        // Race-condition protection: ensure new email is still unique
        var existing = userRepository.findByEmailIgnoreCaseAndNotDeleted(tokenEntity.getNewEmail());
        if (existing.isPresent() && !existing.get().getId().equals(tokenEntity.getUserId())) {
            throw new EmailAlreadyInUseException("Email already in use");
        }

        User user = userRepository.findByIdAndNotDeleted(tokenEntity.getUserId())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // Optional: ensure old email still matches what's stored on user to avoid stale tokens applying to a changed account
        if (user.getEmail() == null || !user.getEmail().equalsIgnoreCase(tokenEntity.getOldEmail())) {
            throw new InvalidEmailResetTokenException("Invalid or expired email reset token");
        }

        user.setEmail(tokenEntity.getNewEmail().toLowerCase());
        userRepository.save(user);

        tokenEntity.markAsUsed();
        emailResetTokenRepository.save(tokenEntity);

        return EmailResetConfirmResponse.builder()
                .newEmail(user.getEmail())
                .build();
    }
}
