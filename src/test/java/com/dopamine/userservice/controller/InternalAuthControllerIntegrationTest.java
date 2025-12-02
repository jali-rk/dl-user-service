package com.dopamine.userservice.controller;

import com.dopamine.userservice.base.BaseIntegrationTest;
import com.dopamine.userservice.domain.User;
import com.dopamine.userservice.domain.VerificationCode;
import com.dopamine.userservice.dto.CredentialsValidationRequest;
import com.dopamine.userservice.repository.UserRepository;
import com.dopamine.userservice.repository.VerificationCodeRepository;
import com.dopamine.userservice.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for InternalAuthController.
 * Tests authentication and password reset flows.
 */
@DisplayName("Internal Auth Controller Integration Tests")
class InternalAuthControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VerificationCodeRepository verificationCodeRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Value("${user.service.internal-token}")
    private String serviceToken;

    private static final String TEST_PASSWORD = "SecurePassword123";

    @BeforeEach
    void setUp() {
        verificationCodeRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should validate credentials successfully for verified student")
    void shouldValidateCredentialsForVerifiedStudent() throws Exception {
        // Given
        String hashedPassword = passwordEncoder.encode(TEST_PASSWORD);
        User student = TestDataBuilder.verifiedStudent()
                .email("login@example.com")
                .passwordHash(hashedPassword)
                .build();
        userRepository.save(student);

        CredentialsValidationRequest request = CredentialsValidationRequest.builder()
                .email("login@example.com")
                .password(TEST_PASSWORD)
                .build();

        // When/Then
        mockMvc.perform(post("/internal/auth/validate-credentials")
                .header("X-Service-Token", serviceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.user.email").value("login@example.com"))
                .andExpect(jsonPath("$.user.isVerified").value(true))
                .andExpect(jsonPath("$.user.lastLoginAt").exists());
    }

    @Test
    @DisplayName("Should reject login for unverified student")
    void shouldRejectUnverifiedStudent() throws Exception {
        // Given
        String hashedPassword = passwordEncoder.encode(TEST_PASSWORD);
        User student = TestDataBuilder.defaultStudent() // Not verified
                .email("unverified@example.com")
                .passwordHash(hashedPassword)
                .build();
        userRepository.save(student);

        CredentialsValidationRequest request = CredentialsValidationRequest.builder()
                .email("unverified@example.com")
                .password(TEST_PASSWORD)
                .build();

        // When/Then
        mockMvc.perform(post("/internal/auth/validate-credentials")
                .header("X-Service-Token", serviceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_NOT_VERIFIED"));
    }

    @Test
    @DisplayName("Should allow login for admin without verification")
    void shouldAllowAdminLoginWithoutVerification() throws Exception {
        // Given
        String hashedPassword = passwordEncoder.encode(TEST_PASSWORD);
        User admin = TestDataBuilder.defaultAdmin()
                .email("admin@example.com")
                .passwordHash(hashedPassword)
                .build();
        userRepository.save(admin);

        CredentialsValidationRequest request = CredentialsValidationRequest.builder()
                .email("admin@example.com")
                .password(TEST_PASSWORD)
                .build();

        // When/Then
        mockMvc.perform(post("/internal/auth/validate-credentials")
                .header("X-Service-Token", serviceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.user.role").value("ADMIN"));
    }

    @Test
    @DisplayName("Should return 401 for wrong password")
    void shouldReturn401ForWrongPassword() throws Exception {
        // Given
        String hashedPassword = passwordEncoder.encode(TEST_PASSWORD);
        User student = TestDataBuilder.verifiedStudent()
                .email("login@example.com")
                .passwordHash(hashedPassword)
                .build();
        userRepository.save(student);

        CredentialsValidationRequest request = CredentialsValidationRequest.builder()
                .email("login@example.com")
                .password("WrongPassword")
                .build();

        // When/Then
        mockMvc.perform(post("/internal/auth/validate-credentials")
                .header("X-Service-Token", serviceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("Should return 401 for non-existent email")
    void shouldReturn401ForNonExistentEmail() throws Exception {
        // Given
        CredentialsValidationRequest request = CredentialsValidationRequest.builder()
                .email("nonexistent@example.com")
                .password(TEST_PASSWORD)
                .build();

        // When/Then
        mockMvc.perform(post("/internal/auth/validate-credentials")
                .header("X-Service-Token", serviceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("Should be case-insensitive for email login")
    void shouldBeCaseInsensitiveForEmail() throws Exception {
        // Given
        String hashedPassword = passwordEncoder.encode(TEST_PASSWORD);
        User student = TestDataBuilder.verifiedStudent()
                .email("test@example.com")
                .passwordHash(hashedPassword)
                .build();
        userRepository.save(student);

        CredentialsValidationRequest request = CredentialsValidationRequest.builder()
                .email("TEST@EXAMPLE.COM") // Different case
                .password(TEST_PASSWORD)
                .build();

        // When/Then
        mockMvc.perform(post("/internal/auth/validate-credentials")
                .header("X-Service-Token", serviceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    @DisplayName("Should request password reset")
    void shouldRequestPasswordReset() throws Exception {
        // Given
        User user = TestDataBuilder.verifiedStudent()
                .email("reset@example.com")
                .build();
        userRepository.save(user);

        String requestJson = """
            {
                "email": "reset@example.com"
            }
            """;

        // When/Then
        mockMvc.perform(post("/internal/auth/password-reset/request")
                .header("X-Service-Token", serviceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.token").exists()); // Raw token returned for BFF to send via email
    }

    @Test
    @DisplayName("Should return generic message for non-existent email in password reset")
    void shouldReturnGenericMessageForNonExistentEmailInReset() throws Exception {
        // Given
        String requestJson = """
            {
                "email": "nonexistent@example.com"
            }
            """;

        // When/Then
        mockMvc.perform(post("/internal/auth/password-reset/request")
                .header("X-Service-Token", serviceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.token").doesNotExist()); // No token for non-existent user
    }

    @Test
    @DisplayName("Should validate email format in credentials request")
    void shouldValidateEmailFormat() throws Exception {
        // Given
        String invalidJson = """
            {
                "email": "invalid-email",
                "password": "password123"
            }
            """;

        // When/Then
        mockMvc.perform(post("/internal/auth/validate-credentials")
                .header("X-Service-Token", serviceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest());
    }
}

