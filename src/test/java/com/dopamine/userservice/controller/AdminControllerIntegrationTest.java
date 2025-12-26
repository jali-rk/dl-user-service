package com.dopamine.userservice.controller;

import com.dopamine.userservice.base.BaseIntegrationTest;
import com.dopamine.userservice.domain.Role;
import com.dopamine.userservice.domain.User;
import com.dopamine.userservice.dto.AdminCreateRequest;
import com.dopamine.userservice.repository.UserRepository;
import com.dopamine.userservice.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AdminController.
 * Tests admin creation and management flows.
 */
@DisplayName("Admin Controller Integration Tests")
class AdminControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Value("${user.service.internal-token}")
    private String serviceToken;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should create admin successfully")
    void shouldCreateAdmin() throws Exception {
        // Given
        AdminCreateRequest request = TestDataBuilder.defaultAdminCreateRequest()
                .email("newadmin@example.com")
                .role(Role.ADMIN)
                .build();

        // When/Then
        mockMvc.perform(post("/admins")
                .header("X-Service-Token", serviceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(request.getEmail().toLowerCase()))
                .andExpect(jsonPath("$.fullName").value(request.getFullName()))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.isVerified").value(true)) // Admins auto-verified
                .andExpect(jsonPath("$.codeNumber").doesNotExist()) // Admins don't have code numbers
                .andExpect(jsonPath("$.passwordHash").doesNotExist()); // Should not expose password
    }

    @Test
    @DisplayName("Should create main admin successfully")
    void shouldCreateMainAdmin() throws Exception {
        // Given
        AdminCreateRequest request = TestDataBuilder.defaultAdminCreateRequest()
                .email("mainadmin@example.com")
                .role(Role.MAIN_ADMIN)
                .build();

        // When/Then
        mockMvc.perform(post("/admins")
                .header("X-Service-Token", serviceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("MAIN_ADMIN"));
    }

    @Test
    @DisplayName("Should return 400 when creating admin with existing email")
    void shouldReturn400WhenAdminEmailExists() throws Exception {
        // Given - Create existing admin
        User existingAdmin = TestDataBuilder.defaultAdmin()
                .email("existing@example.com")
                .build();
        userRepository.save(existingAdmin);

        AdminCreateRequest request = TestDataBuilder.defaultAdminCreateRequest()
                .email("existing@example.com")
                .build();

        // When/Then
        mockMvc.perform(post("/admins")
                .header("X-Service-Token", serviceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("USER_ALREADY_EXISTS"));
    }

    @Test
    @DisplayName("Should get admin by ID")
    void shouldGetAdminById() throws Exception {
        // Given
        User admin = TestDataBuilder.defaultAdmin()
                .email("get@example.com")
                .build();
        admin = userRepository.save(admin);

        // When/Then
        mockMvc.perform(get("/admins/{adminId}", admin.getId())
                .header("X-Service-Token", serviceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(admin.getId().toString()))
                .andExpect(jsonPath("$.email").value(admin.getEmail()))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @DisplayName("Should return 404 when admin not found")
    void shouldReturn404WhenAdminNotFound() throws Exception {
        // Given
        String nonExistentId = "550e8400-e29b-41d4-a716-446655440000";

        // When/Then
        mockMvc.perform(get("/admins/{adminId}", nonExistentId)
                .header("X-Service-Token", serviceToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    @DisplayName("Should update admin profile")
    void shouldUpdateAdminProfile() throws Exception {
        // Given
        User admin = TestDataBuilder.defaultAdmin()
                .email("update@example.com")
                .build();
        admin = userRepository.save(admin);

        String updateJson = """
            {
                "fullName": "Updated Admin Name"
            }
            """;

        // When/Then
        mockMvc.perform(patch("/admins/{adminId}", admin.getId())
                .header("X-Service-Token", serviceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Updated Admin Name"))
                .andExpect(jsonPath("$.email").value(admin.getEmail())); // Email shouldn't change
    }

    @Test
    @DisplayName("Should return 404 when trying to get student as admin")
    void shouldReturn404WhenGettingStudentAsAdmin() throws Exception {
        // Given - Create a student (not admin)
        User student = TestDataBuilder.verifiedStudent()
                .email("student@example.com")
                .build();
        student = userRepository.save(student);

        // When/Then - Try to get as admin
        mockMvc.perform(get("/admins/{adminId}", student.getId())
                .header("X-Service-Token", serviceToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    @DisplayName("Should validate required fields in admin creation request")
    void shouldValidateAdminCreationRequest() throws Exception {
        // Given - Missing required fields
        String invalidJson = """
            {
                "email": "invalid-email",
                "password": ""
            }
            """;

        // When/Then
        mockMvc.perform(post("/admins")
                .header("X-Service-Token", serviceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should delete admin (soft delete) and return 204")
    void shouldDeleteAdmin() throws Exception {
        // Given
        User admin = TestDataBuilder.defaultAdmin()
                .email("delete@example.com")
                .build();
        admin = userRepository.save(admin);

        // When/Then - delete
        mockMvc.perform(delete("/admins/{adminId}", admin.getId())
                .header("X-Service-Token", serviceToken))
                .andExpect(status().isNoContent());

        // Then - should not be retrievable anymore
        mockMvc.perform(get("/admins/{adminId}", admin.getId())
                .header("X-Service-Token", serviceToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));

        // And - verify soft delete persisted
        User deleted = userRepository.findById(admin.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(deleted.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should return 404 when deleting non-existent admin")
    void shouldReturn404WhenDeletingAdminNotFound() throws Exception {
        String nonExistentId = "550e8400-e29b-41d4-a716-446655440000";

        mockMvc.perform(delete("/admins/{adminId}", nonExistentId)
                .header("X-Service-Token", serviceToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }
}
