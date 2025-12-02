package com.dopamine.userservice.controller;

import com.dopamine.userservice.base.BaseIntegrationTest;
import com.dopamine.userservice.domain.Role;
import com.dopamine.userservice.domain.User;
import com.dopamine.userservice.domain.UserStatus;
import com.dopamine.userservice.domain.VerificationCode;
import com.dopamine.userservice.domain.VerificationType;
import com.dopamine.userservice.dto.StudentRegistrationRequest;
import com.dopamine.userservice.dto.VerifyCodeRequest;
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

import java.time.Duration;
import java.time.Instant;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for StudentController.
 * Tests the full flow from HTTP request to database.
 */
@DisplayName("Student Controller Integration Tests")
class StudentControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VerificationCodeRepository verificationCodeRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Value("${user.service.internal-token}")
    private String serviceToken;

    @BeforeEach
    void setUp() {
        verificationCodeRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should register student and create verification code")
    void shouldRegisterStudent() throws Exception {
        // Given
        StudentRegistrationRequest request = TestDataBuilder.defaultStudentRegistrationRequest()
                .email("newstudent@example.com")
                .build();

        // When/Then
        mockMvc.perform(post("/students/registrations")
                .header("X-Service-Token", serviceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.verificationCodeGenerated").value(true))
                .andExpect(jsonPath("$.user.email").value(request.getEmail().toLowerCase()))
                .andExpect(jsonPath("$.user.fullName").value(request.getFullName()))
                .andExpect(jsonPath("$.user.role").value("STUDENT"))
                .andExpect(jsonPath("$.user.isVerified").value(false))
                .andExpect(jsonPath("$.user.codeNumber").exists())
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist()); // Should not expose password
    }

    @Test
    @DisplayName("Should return 400 when registering with existing email")
    void shouldReturn400WhenEmailExists() throws Exception {
        // Given - Create existing user
        User existingUser = TestDataBuilder.defaultStudent()
                .email("existing@example.com")
                .build();
        userRepository.save(existingUser);

        StudentRegistrationRequest request = TestDataBuilder.defaultStudentRegistrationRequest()
                .email("EXISTING@EXAMPLE.COM") // Case-insensitive check
                .build();

        // When/Then
        mockMvc.perform(post("/students/registrations")
                .header("X-Service-Token", serviceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("USER_ALREADY_EXISTS"));
    }

    @Test
    @DisplayName("Should verify student code successfully")
    void shouldVerifyStudentCode() throws Exception {
        // Given - Create unverified student
        User student = TestDataBuilder.defaultStudent()
                .email("verify@example.com")
                .codeNumber("1001")
                .isVerified(false)
                .build();
        student = userRepository.save(student);

        VerificationCode code = TestDataBuilder.defaultVerificationCode(student.getId())
                .code("1001")
                .build();
        verificationCodeRepository.save(code);

        VerifyCodeRequest request = VerifyCodeRequest.builder()
                .email("verify@example.com")
                .code("1001")
                .build();

        // When/Then
        mockMvc.perform(post("/students/verify-code")
                .header("X-Service-Token", serviceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isVerified").value(true))
                .andExpect(jsonPath("$.email").value("verify@example.com"));
    }

    @Test
    @DisplayName("Should return 400 for invalid verification code")
    void shouldReturn400ForInvalidCode() throws Exception {
        // Given
        User student = TestDataBuilder.defaultStudent()
                .email("verify@example.com")
                .build();
        student = userRepository.save(student);

        VerificationCode code = TestDataBuilder.defaultVerificationCode(student.getId())
                .code("1001")
                .build();
        verificationCodeRepository.save(code);

        VerifyCodeRequest request = VerifyCodeRequest.builder()
                .email("verify@example.com")
                .code("WRONG_CODE")
                .build();

        // When/Then
        mockMvc.perform(post("/students/verify-code")
                .header("X-Service-Token", serviceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_VERIFICATION_CODE"));
    }

    @Test
    @DisplayName("Should get student by ID")
    void shouldGetStudentById() throws Exception {
        // Given
        User student = TestDataBuilder.verifiedStudent()
                .email("get@example.com")
                .build();
        student = userRepository.save(student);

        // When/Then
        mockMvc.perform(get("/students/{studentId}", student.getId())
                .header("X-Service-Token", serviceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(student.getId().toString()))
                .andExpect(jsonPath("$.email").value(student.getEmail()))
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    @DisplayName("Should return 404 when student not found")
    void shouldReturn404WhenStudentNotFound() throws Exception {
        // Given
        String nonExistentId = "550e8400-e29b-41d4-a716-446655440000";

        // When/Then
        mockMvc.perform(get("/students/{studentId}", nonExistentId)
                .header("X-Service-Token", serviceToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    @DisplayName("Should update student profile")
    void shouldUpdateStudentProfile() throws Exception {
        // Given
        User student = TestDataBuilder.verifiedStudent()
                .email("update@example.com")
                .build();
        student = userRepository.save(student);

        String updateJson = """
            {
                "fullName": "Updated Name",
                "whatsappNumber": "+94771111111",
                "school": "New School",
                "address": "New Address"
            }
            """;

        // When/Then
        mockMvc.perform(patch("/students/{studentId}", student.getId())
                .header("X-Service-Token", serviceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Updated Name"))
                .andExpect(jsonPath("$.whatsappNumber").value("+94771111111"))
                .andExpect(jsonPath("$.school").value("New School"))
                .andExpect(jsonPath("$.address").value("New Address"))
                .andExpect(jsonPath("$.email").value(student.getEmail())); // Email shouldn't change
    }

    @Test
    @DisplayName("Should return 401 without service token")
    void shouldReturn401WithoutServiceToken() throws Exception {
        // When/Then
        mockMvc.perform(get("/students"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 401 with invalid service token")
    void shouldReturn401WithInvalidToken() throws Exception {
        // When/Then
        mockMvc.perform(get("/students")
                .header("X-Service-Token", "invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should validate required fields in registration request")
    void shouldValidateRegistrationRequest() throws Exception {
        // Given - Missing required fields
        String invalidJson = """
            {
                "email": "invalid-email",
                "password": ""
            }
            """;

        // When/Then
        mockMvc.perform(post("/students/registrations")
                .header("X-Service-Token", serviceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest());
    }
}

