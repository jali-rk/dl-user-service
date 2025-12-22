package com.dopamine.userservice.service.impl;

import com.dopamine.userservice.domain.*;
import com.dopamine.userservice.dto.*;
import com.dopamine.userservice.exception.*;
import com.dopamine.userservice.mapper.UserMapper;
import com.dopamine.userservice.repository.PasswordResetTokenRepository;
import com.dopamine.userservice.repository.UserRepository;
import com.dopamine.userservice.repository.VerificationCodeRepository;
import com.dopamine.userservice.service.StudentCodeGeneratorService;
import com.dopamine.userservice.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
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
    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final StudentCodeGeneratorService studentCodeGeneratorService;

    public UserServiceImpl(
            UserRepository userRepository,
            VerificationCodeRepository verificationCodeRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            UserMapper userMapper,
            BCryptPasswordEncoder passwordEncoder,
            StudentCodeGeneratorService studentCodeGeneratorService
    ) {
        this.userRepository = userRepository;
        this.verificationCodeRepository = verificationCodeRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.studentCodeGeneratorService = studentCodeGeneratorService;
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
                .expiresAt(Instant.now().plus(Duration.ofMinutes(2)))
                .retryCount(0)
                .build();

        verificationCodeRepository.save(verificationCode);
        log.info("Created verification code for user: {}", user.getId());

        // TODO: Send notification with registration number to student's email
        // For now, just log the notification intent
        log.info("TODO: Send verification code {} to email {}", registrationNumber, user.getEmail());

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
                // Generate random token
                String rawToken = UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
                String tokenHash = passwordEncoder.encode(rawToken);

                // Create password reset token
                PasswordResetToken token = PasswordResetToken.builder()
                        .userId(user.getId())
                        .tokenHash(tokenHash)
                        .expiresAt(Instant.now().plus(Duration.ofMinutes(30)))
                        .used(false)
                        .build();

                passwordResetTokenRepository.save(token);
                log.info("Created password reset token for user: {}", user.getId());

                // TODO: Send email with reset link containing rawToken
                log.info("TODO: Send password reset email to {} with token", user.getEmail());

                return PasswordResetResponse.builder()
                        .message("Password reset instructions have been sent to your email")
                        .token(rawToken) // Return raw token for BFF to send via email
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

        // Hash the provided token to match against stored hash
        // Note: We need to find by token differently since BCrypt generates different hashes
        // Better approach: Store token ID separately and use that for lookup
        // For now, we'll iterate through recent tokens (not ideal for production)

        // TODO: Improve token lookup strategy - consider storing token ID separately
        String tokenHash = passwordEncoder.encode(request.getToken());

        PasswordResetToken token = passwordResetTokenRepository.findValidByTokenHash(tokenHash, Instant.now())
                .orElseThrow(() -> new InvalidPasswordResetTokenException("Invalid or expired password reset token"));

        // Verify token is valid
        if (!token.isValid()) {
            log.warn("Invalid password reset token attempt");
            throw new InvalidPasswordResetTokenException("Invalid or expired password reset token");
        }

        // Find user
        User user = userRepository.findByIdAndNotDeleted(token.getUserId())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // Update password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Mark token as used
        token.markAsUsed();
        passwordResetTokenRepository.save(token);

        log.info("Successfully reset password for user: {}", user.getId());
    }
}

