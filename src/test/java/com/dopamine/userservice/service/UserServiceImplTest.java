package com.dopamine.userservice.service;

import com.dopamine.userservice.domain.*;
import com.dopamine.userservice.dto.*;
import com.dopamine.userservice.exception.*;
import com.dopamine.userservice.mapper.UserMapper;
import com.dopamine.userservice.repository.PasswordResetTokenRepository;
import com.dopamine.userservice.repository.UserRepository;
import com.dopamine.userservice.repository.VerificationCodeRepository;
import com.dopamine.userservice.service.impl.UserServiceImpl;
import com.dopamine.userservice.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserServiceImpl.
 * Tests business logic in isolation using mocked dependencies.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private VerificationCodeRepository verificationCodeRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    // Use real UserMapper instead of mock (Java 21+ compatibility)
    private final UserMapper userMapper = new UserMapper();

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private StudentCodeGeneratorService studentCodeGeneratorService;

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void setUpService() {
        // Manually inject the real UserMapper since we can't use @InjectMocks for it
        userService = new UserServiceImpl(
                userRepository,
                verificationCodeRepository,
                passwordResetTokenRepository,
                userMapper,
                passwordEncoder,
                studentCodeGeneratorService
        );
    }

    @Nested
    @DisplayName("Student Registration Tests")
    class StudentRegistrationTests {

        @Test
        @DisplayName("Should register student successfully with generated code")
        void shouldRegisterStudentSuccessfully() {
            // Given
            StudentRegistrationRequest request = TestDataBuilder.defaultStudentRegistrationRequest().build();
            String generatedCode = "560001";
            String hashedPassword = "$2a$10$hashedPassword";

            when(userRepository.existsByEmailIgnoreCaseAndNotDeleted(request.getEmail())).thenReturn(false);
            when(studentCodeGeneratorService.generateStudentCode()).thenReturn(generatedCode);
            when(passwordEncoder.encode(request.getPassword())).thenReturn(hashedPassword);

            User savedUser = TestDataBuilder.defaultStudent()
                    .email(request.getEmail())
                    .codeNumber(generatedCode)
                    .build();
            when(userRepository.save(any(User.class))).thenReturn(savedUser);


            // When
            StudentRegistrationResponse response = userService.registerStudent(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isVerificationCodeGenerated()).isTrue();
            assertThat(response.getUser()).isNotNull();
            assertThat(response.getUser().getEmail()).isEqualTo(request.getEmail().toLowerCase());
            assertThat(response.getUser().getCodeNumber()).isEqualTo(generatedCode);
            assertThat(response.getUser().isVerified()).isFalse();

            // Verify interactions
            verify(userRepository).existsByEmailIgnoreCaseAndNotDeleted(request.getEmail());
            verify(studentCodeGeneratorService).generateStudentCode();
            verify(passwordEncoder).encode(request.getPassword());

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User capturedUser = userCaptor.getValue();
            assertThat(capturedUser.getCodeNumber()).isEqualTo(generatedCode);
            assertThat(capturedUser.getRole()).isEqualTo(Role.STUDENT);
            assertThat(capturedUser.isVerified()).isFalse();

            ArgumentCaptor<VerificationCode> codeCaptor = ArgumentCaptor.forClass(VerificationCode.class);
            verify(verificationCodeRepository).save(codeCaptor.capture());
            VerificationCode capturedCode = codeCaptor.getValue();
            assertThat(capturedCode.getCode()).isEqualTo(generatedCode);
            assertThat(capturedCode.getType()).isEqualTo(VerificationType.REGISTRATION);
            assertThat(capturedCode.getRetryCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should throw exception when email already exists")
        void shouldThrowExceptionWhenEmailExists() {
            // Given
            StudentRegistrationRequest request = TestDataBuilder.defaultStudentRegistrationRequest().build();
            when(userRepository.existsByEmailIgnoreCaseAndNotDeleted(request.getEmail())).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> userService.registerStudent(request))
                    .isInstanceOf(UserAlreadyExistsException.class)
                    .hasMessageContaining(request.getEmail());

            verify(userRepository, never()).save(any(User.class));
            verify(verificationCodeRepository, never()).save(any(VerificationCode.class));
        }

        @Test
        @DisplayName("Should convert email to lowercase")
        void shouldConvertEmailToLowercase() {
            // Given
            StudentRegistrationRequest request = TestDataBuilder.defaultStudentRegistrationRequest()
                    .email("TEST@EXAMPLE.COM")
                    .build();

            when(userRepository.existsByEmailIgnoreCaseAndNotDeleted(anyString())).thenReturn(false);
            when(studentCodeGeneratorService.generateStudentCode()).thenReturn("560001");
            when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
            when(userRepository.save(any(User.class))).thenReturn(TestDataBuilder.defaultStudent().build());

            // When
            userService.registerStudent(request);

            // Then
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getEmail()).isEqualTo("test@example.com");
        }
    }

    @Nested
    @DisplayName("Student Verification Tests")
    class StudentVerificationTests {

        @Test
        @DisplayName("Should verify student with valid code")
        void shouldVerifyStudentWithValidCode() {
            // Given
            VerifyCodeRequest request = TestDataBuilder.defaultVerifyCodeRequest().build();
            User user = TestDataBuilder.defaultStudent().build();
            VerificationCode verificationCode = TestDataBuilder.defaultVerificationCode(user.getId()).build();

            when(userRepository.findByEmailIgnoreCaseAndNotDeleted(request.getEmail()))
                    .thenReturn(Optional.of(user));
            when(verificationCodeRepository.findLatestActiveByUserIdAndType(
                    eq(user.getId()), eq(VerificationType.REGISTRATION), any(Instant.class)))
                    .thenReturn(Optional.of(verificationCode));
            when(userRepository.save(any(User.class))).thenReturn(user);

            // When
            UserPublicView result = userService.verifyStudentCode(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo(user.getEmail());
            verify(verificationCodeRepository).save(argThat(code -> code.getConsumedAt() != null));
            verify(userRepository).save(argThat(u -> u.isVerified()));
        }

        @Test
        @DisplayName("Should return user view if already verified")
        void shouldReturnUserIfAlreadyVerified() {
            // Given
            VerifyCodeRequest request = TestDataBuilder.defaultVerifyCodeRequest().build();
            User user = TestDataBuilder.verifiedStudent().build();

            when(userRepository.findByEmailIgnoreCaseAndNotDeleted(request.getEmail()))
                    .thenReturn(Optional.of(user));

            // When
            UserPublicView result = userService.verifyStudentCode(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.isVerified()).isTrue();
            verify(verificationCodeRepository, never()).findLatestActiveByUserIdAndType(any(), any(), any());
        }

        @Test
        @DisplayName("Should throw exception for invalid code")
        void shouldThrowExceptionForInvalidCode() {
            // Given
            VerifyCodeRequest request = TestDataBuilder.defaultVerifyCodeRequest()
                    .code("WRONG_CODE")
                    .build();
            User user = TestDataBuilder.defaultStudent().build();
            VerificationCode verificationCode = TestDataBuilder.defaultVerificationCode(user.getId()).build();

            when(userRepository.findByEmailIgnoreCaseAndNotDeleted(request.getEmail()))
                    .thenReturn(Optional.of(user));
            when(verificationCodeRepository.findLatestActiveByUserIdAndType(
                    eq(user.getId()), eq(VerificationType.REGISTRATION), any(Instant.class)))
                    .thenReturn(Optional.of(verificationCode));

            // When/Then
            assertThatThrownBy(() -> userService.verifyStudentCode(request))
                    .isInstanceOf(InvalidVerificationCodeException.class);

            verify(verificationCodeRepository).save(argThat(code -> code.getRetryCount() == 1));
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should mark code as consumed after 3 failed attempts")
        void shouldMarkCodeConsumedAfterThreeFailedAttempts() {
            // Given
            VerifyCodeRequest request = TestDataBuilder.defaultVerifyCodeRequest()
                    .code("WRONG_CODE")
                    .build();
            User user = TestDataBuilder.defaultStudent().build();
            VerificationCode verificationCode = TestDataBuilder.defaultVerificationCode(user.getId())
                    .retryCount(2) // Already 2 failed attempts
                    .build();

            when(userRepository.findByEmailIgnoreCaseAndNotDeleted(request.getEmail()))
                    .thenReturn(Optional.of(user));
            when(verificationCodeRepository.findLatestActiveByUserIdAndType(
                    eq(user.getId()), eq(VerificationType.REGISTRATION), any(Instant.class)))
                    .thenReturn(Optional.of(verificationCode));

            // When/Then
            assertThatThrownBy(() -> userService.verifyStudentCode(request))
                    .isInstanceOf(InvalidVerificationCodeException.class);

            verify(verificationCodeRepository).save(argThat(code ->
                code.getRetryCount() == 3 && code.getConsumedAt() != null
            ));
        }

        @Test
        @DisplayName("Should throw exception when no active verification code found")
        void shouldThrowExceptionWhenNoActiveCode() {
            // Given
            VerifyCodeRequest request = TestDataBuilder.defaultVerifyCodeRequest().build();
            User user = TestDataBuilder.defaultStudent().build();

            when(userRepository.findByEmailIgnoreCaseAndNotDeleted(request.getEmail()))
                    .thenReturn(Optional.of(user));
            when(verificationCodeRepository.findLatestActiveByUserIdAndType(
                    eq(user.getId()), eq(VerificationType.REGISTRATION), any(Instant.class)))
                    .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> userService.verifyStudentCode(request))
                    .isInstanceOf(InvalidVerificationCodeException.class)
                    .hasMessageContaining("No active verification code found");
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            VerifyCodeRequest request = TestDataBuilder.defaultVerifyCodeRequest().build();
            when(userRepository.findByEmailIgnoreCaseAndNotDeleted(request.getEmail()))
                    .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> userService.verifyStudentCode(request))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Credential Validation Tests")
    class CredentialValidationTests {

        @Test
        @DisplayName("Should validate credentials successfully for verified student")
        void shouldValidateCredentialsSuccessfully() {
            // Given
            CredentialsValidationRequest request = TestDataBuilder.defaultCredentialsRequest().build();
            User user = TestDataBuilder.verifiedStudent().build();

            when(userRepository.findByEmailIgnoreCaseAndNotDeleted(request.getEmail()))
                    .thenReturn(Optional.of(user));
            when(passwordEncoder.matches(request.getPassword(), user.getPasswordHash()))
                    .thenReturn(true);
            when(userRepository.save(any(User.class))).thenReturn(user);

            // When
            CredentialsValidationResponse response = userService.validateCredentials(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isValid()).isTrue();
            assertThat(response.getUser()).isNotNull();
            assertThat(response.getUser().getEmail()).isEqualTo(user.getEmail());

            verify(userRepository).save(argThat(u -> u.getLastLoginAt() != null));
        }

        @Test
        @DisplayName("Should throw exception for invalid password")
        void shouldThrowExceptionForInvalidPassword() {
            // Given
            CredentialsValidationRequest request = TestDataBuilder.defaultCredentialsRequest().build();
            User user = TestDataBuilder.verifiedStudent().build();

            when(userRepository.findByEmailIgnoreCaseAndNotDeleted(request.getEmail()))
                    .thenReturn(Optional.of(user));
            when(passwordEncoder.matches(request.getPassword(), user.getPasswordHash()))
                    .thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> userService.validateCredentials(request))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessageContaining("Invalid credentials");

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception for unverified student")
        void shouldThrowExceptionForUnverifiedStudent() {
            // Given
            CredentialsValidationRequest request = TestDataBuilder.defaultCredentialsRequest().build();
            User user = TestDataBuilder.defaultStudent().build(); // Not verified

            when(userRepository.findByEmailIgnoreCaseAndNotDeleted(request.getEmail()))
                    .thenReturn(Optional.of(user));
            when(passwordEncoder.matches(request.getPassword(), user.getPasswordHash()))
                    .thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> userService.validateCredentials(request))
                    .isInstanceOf(UnverifiedUserException.class)
                    .hasMessageContaining("not verified");

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should allow login for admin without verification check")
        void shouldAllowAdminLoginWithoutVerificationCheck() {
            // Given
            CredentialsValidationRequest request = TestDataBuilder.defaultCredentialsRequest().build();
            User admin = TestDataBuilder.defaultAdmin()
                    .isVerified(false) // Even if not verified
                    .build();

            when(userRepository.findByEmailIgnoreCaseAndNotDeleted(request.getEmail()))
                    .thenReturn(Optional.of(admin));
            when(passwordEncoder.matches(request.getPassword(), admin.getPasswordHash()))
                    .thenReturn(true);
            when(userRepository.save(any(User.class))).thenReturn(admin);

            // When
            CredentialsValidationResponse response = userService.validateCredentials(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isValid()).isTrue();
        }

        @Test
        @DisplayName("Should throw exception for inactive user")
        void shouldThrowExceptionForInactiveUser() {
            // Given
            CredentialsValidationRequest request = TestDataBuilder.defaultCredentialsRequest().build();
            User user = TestDataBuilder.verifiedStudent()
                    .status(UserStatus.INACTIVE)
                    .build();

            when(userRepository.findByEmailIgnoreCaseAndNotDeleted(request.getEmail()))
                    .thenReturn(Optional.of(user));
            when(passwordEncoder.matches(request.getPassword(), user.getPasswordHash()))
                    .thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> userService.validateCredentials(request))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessageContaining("not active");
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            CredentialsValidationRequest request = TestDataBuilder.defaultCredentialsRequest().build();
            when(userRepository.findByEmailIgnoreCaseAndNotDeleted(request.getEmail()))
                    .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> userService.validateCredentials(request))
                    .isInstanceOf(InvalidCredentialsException.class);
        }
    }

    @Nested
    @DisplayName("Password Reset Tests")
    class PasswordResetTests {

        @Test
        @DisplayName("Should create password reset token for existing user")
        void shouldCreatePasswordResetToken() {
            // Given
            PasswordResetRequest request = TestDataBuilder.defaultPasswordResetRequest().build();
            User user = TestDataBuilder.verifiedStudent().build();

            when(userRepository.findByEmailIgnoreCaseAndNotDeleted(request.getEmail()))
                    .thenReturn(Optional.of(user));
            when(passwordEncoder.encode(anyString())).thenReturn("hashedToken");

            // When
            PasswordResetResponse response = userService.requestPasswordReset(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getMessage()).contains("Password reset instructions");
            assertThat(response.getToken()).isNotNull();

            verify(passwordResetTokenRepository).save(argThat(token ->
                token.getUserId().equals(user.getId()) &&
                !token.isUsed() &&
                token.getExpiresAt().isAfter(Instant.now())
            ));
        }

        @Test
        @DisplayName("Should return generic message for non-existent email")
        void shouldReturnGenericMessageForNonExistentEmail() {
            // Given
            PasswordResetRequest request = TestDataBuilder.defaultPasswordResetRequest().build();
            when(userRepository.findByEmailIgnoreCaseAndNotDeleted(request.getEmail()))
                    .thenReturn(Optional.empty());

            // When
            PasswordResetResponse response = userService.requestPasswordReset(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getToken()).isNull();
            verify(passwordResetTokenRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Student Update Tests")
    class StudentUpdateTests {

        @Test
        @DisplayName("Should update student profile successfully")
        void shouldUpdateStudentProfile() {
            // Given
            UUID studentId = UUID.randomUUID();
            StudentUpdateRequest request = TestDataBuilder.defaultStudentUpdateRequest().build();
            User student = TestDataBuilder.verifiedStudent().id(studentId).build();

            when(userRepository.findByIdAndNotDeleted(studentId)).thenReturn(Optional.of(student));
            when(userRepository.save(any(User.class))).thenReturn(student);

            // When
            UserPublicView result = userService.updateStudent(studentId, request);

            // Then
            assertThat(result).isNotNull();
            verify(userRepository).save(argThat(u ->
                u.getFullName().equals(request.getFullName()) &&
                u.getWhatsappNumber().equals(request.getWhatsappNumber()) &&
                u.getSchool().equals(request.getSchool()) &&
                u.getAddress().equals(request.getAddress())
            ));
        }

        @Test
        @DisplayName("Should throw exception when updating non-student user")
        void shouldThrowExceptionWhenUpdatingNonStudent() {
            // Given
            UUID adminId = UUID.randomUUID();
            StudentUpdateRequest request = TestDataBuilder.defaultStudentUpdateRequest().build();
            User admin = TestDataBuilder.defaultAdmin().id(adminId).build();

            when(userRepository.findByIdAndNotDeleted(adminId)).thenReturn(Optional.of(admin));

            // When/Then
            assertThatThrownBy(() -> userService.updateStudent(adminId, request))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining("not a student");
        }
    }

    @Nested
    @DisplayName("Admin Creation Tests")
    class AdminCreationTests {

        @Test
        @DisplayName("Should create admin successfully")
        void shouldCreateAdminSuccessfully() {
            // Given
            AdminCreateRequest request = TestDataBuilder.defaultAdminCreateRequest().build();
            when(userRepository.existsByEmailIgnoreCaseAndNotDeleted(request.getEmail())).thenReturn(false);
            when(passwordEncoder.encode(request.getPassword())).thenReturn("hashedPassword");

            User savedAdmin = TestDataBuilder.defaultAdmin().build();
            when(userRepository.save(any(User.class))).thenReturn(savedAdmin);

            // When
            UserPublicView result = userService.createAdmin(request);

            // Then
            assertThat(result).isNotNull();
            verify(userRepository).save(argThat(u ->
                u.getRole() == Role.ADMIN &&
                u.isVerified() &&
                u.getCodeNumber() == null
            ));
        }

        @Test
        @DisplayName("Should throw exception when admin email already exists")
        void shouldThrowExceptionWhenAdminEmailExists() {
            // Given
            AdminCreateRequest request = TestDataBuilder.defaultAdminCreateRequest().build();
            when(userRepository.existsByEmailIgnoreCaseAndNotDeleted(request.getEmail())).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> userService.createAdmin(request))
                    .isInstanceOf(UserAlreadyExistsException.class);
        }
    }
}

