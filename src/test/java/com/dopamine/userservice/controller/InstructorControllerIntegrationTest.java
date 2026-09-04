package com.dopamine.userservice.controller;

import com.dopamine.userservice.base.BaseIntegrationTest;
import com.dopamine.userservice.domain.User;
import com.dopamine.userservice.repository.UserRepository;
import com.dopamine.userservice.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for InstructorController.
 * Tests instructor retrieval, update, and deletion flows.
 * Instructor creation is covered by AdminControllerIntegrationTest (POST /admins with role INSTRUCTOR).
 */
@DisplayName("Instructor Controller Integration Tests")
class InstructorControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Value("${user.service.internal-token}")
    private String serviceToken;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should get instructor by ID")
    void shouldGetInstructorById() throws Exception {
        // Given
        User instructor = TestDataBuilder.defaultInstructor()
                .email("get-instructor@example.com")
                .build();
        instructor = userRepository.save(instructor);

        // When/Then
        mockMvc.perform(get("/instructors/{instructorId}", instructor.getId())
                .header("X-Service-Token", serviceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(instructor.getId().toString()))
                .andExpect(jsonPath("$.email").value(instructor.getEmail()))
                .andExpect(jsonPath("$.role").value("INSTRUCTOR"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("Should return 404 when instructor not found")
    void shouldReturn404WhenInstructorNotFound() throws Exception {
        // Given
        String nonExistentId = "550e8400-e29b-41d4-a716-446655440000";

        // When/Then
        mockMvc.perform(get("/instructors/{instructorId}", nonExistentId)
                .header("X-Service-Token", serviceToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    @DisplayName("Should return 404 when trying to get an admin as an instructor")
    void shouldReturn404WhenGettingAdminAsInstructor() throws Exception {
        // Given - Create an admin (not instructor)
        User admin = TestDataBuilder.defaultAdmin()
                .email("admin-not-instructor@example.com")
                .build();
        admin = userRepository.save(admin);

        // When/Then
        mockMvc.perform(get("/instructors/{instructorId}", admin.getId())
                .header("X-Service-Token", serviceToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    @DisplayName("Should update instructor profile")
    void shouldUpdateInstructorProfile() throws Exception {
        // Given
        User instructor = TestDataBuilder.defaultInstructor()
                .email("update-instructor@example.com")
                .build();
        instructor = userRepository.save(instructor);

        String updateJson = """
            {
                "fullName": "Updated Instructor Name"
            }
            """;

        // When/Then
        mockMvc.perform(patch("/instructors/{instructorId}", instructor.getId())
                .header("X-Service-Token", serviceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Updated Instructor Name"))
                .andExpect(jsonPath("$.email").value(instructor.getEmail())); // Email shouldn't change
    }

    @Test
    @DisplayName("Should return 404 when updating non-existent instructor")
    void shouldReturn404WhenUpdatingInstructorNotFound() throws Exception {
        String nonExistentId = "550e8400-e29b-41d4-a716-446655440000";

        String updateJson = """
            {
                "fullName": "Doesn't matter"
            }
            """;

        mockMvc.perform(patch("/instructors/{instructorId}", nonExistentId)
                .header("X-Service-Token", serviceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    @DisplayName("Should delete instructor (soft delete) and return 204")
    void shouldDeleteInstructor() throws Exception {
        // Given
        User instructor = TestDataBuilder.defaultInstructor()
                .email("delete-instructor@example.com")
                .build();
        instructor = userRepository.save(instructor);

        // When/Then - delete
        mockMvc.perform(delete("/instructors/{instructorId}", instructor.getId())
                .header("X-Service-Token", serviceToken))
                .andExpect(status().isNoContent());

        // Then - should not be retrievable anymore
        mockMvc.perform(get("/instructors/{instructorId}", instructor.getId())
                .header("X-Service-Token", serviceToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));

        // And - verify soft delete persisted
        User deleted = userRepository.findById(instructor.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(deleted.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should return 404 when deleting non-existent instructor")
    void shouldReturn404WhenDeletingInstructorNotFound() throws Exception {
        String nonExistentId = "550e8400-e29b-41d4-a716-446655440000";

        mockMvc.perform(delete("/instructors/{instructorId}", nonExistentId)
                .header("X-Service-Token", serviceToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }
}
