package com.dopamine.userservice.controller;

import com.dopamine.userservice.base.BaseIntegrationTest;
import com.dopamine.userservice.domain.Role;
import com.dopamine.userservice.domain.User;
import com.dopamine.userservice.domain.UserStatus;
import com.dopamine.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for GET /students endpoint with pagination.
 */
@AutoConfigureMockMvc
@Transactional
@DisplayName("Get Paginated Students Integration Tests")
class GetAllStudentsIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should return paginated active and verified students")
    void shouldReturnPaginatedActiveVerifiedStudents() throws Exception {
        // Given - Create 5 active and verified students
        for (int i = 1; i <= 5; i++) {
            User student = User.builder()
                    .fullName("Active Student " + i)
                    .email("active" + i + "@example.com")
                    .whatsappNumber("+94770000" + i)
                    .school("Test School " + i)
                    .address("Test Address " + i)
                    .role(Role.STUDENT)
                    .status(UserStatus.ACTIVE)
                    .codeNumber("56000" + i)
                    .isVerified(true)
                    .passwordHash(passwordEncoder.encode("password123"))
                    .build();
            userRepository.save(student);
        }

        // When & Then - Request page 1 with pageSize 3
        mockMvc.perform(get("/students")
                        .param("page", "1")
                        .param("pageSize", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(3)))
                .andExpect(jsonPath("$.total", is(5)))
                .andExpect(jsonPath("$.items[*].role", everyItem(is("STUDENT"))))
                .andExpect(jsonPath("$.items[*].isVerified", everyItem(is(true))));
    }

    @Test
    @DisplayName("Should validate page parameter minimum value")
    void shouldValidatePageMinimum() throws Exception {
        mockMvc.perform(get("/students")
                        .param("page", "0")
                        .param("pageSize", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should validate pageSize parameter minimum value")
    void shouldValidatePageSizeMinimum() throws Exception {
        mockMvc.perform(get("/students")
                        .param("page", "1")
                        .param("pageSize", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should validate pageSize parameter maximum value")
    void shouldValidatePageSizeMaximum() throws Exception {
        mockMvc.perform(get("/students")
                        .param("page", "1")
                        .param("pageSize", "101"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should require page parameter")
    void shouldRequirePageParameter() throws Exception {
        mockMvc.perform(get("/students")
                        .param("pageSize", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should require pageSize parameter")
    void shouldRequirePageSizeParameter() throws Exception {
        mockMvc.perform(get("/students")
                        .param("page", "1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should exclude inactive students")
    void shouldExcludeInactiveStudents() throws Exception {
        // Given - 2 active students and 1 inactive student
        User activeStudent1 = User.builder()
                .fullName("Active Student 1")
                .email("active1@example.com")
                .whatsappNumber("+94770001")
                .school("Test School")
                .address("Test Address")
                .role(Role.STUDENT)
                .status(UserStatus.ACTIVE)
                .codeNumber("560001")
                .isVerified(true)
                .passwordHash(passwordEncoder.encode("password123"))
                .build();
        userRepository.save(activeStudent1);

        User inactiveStudent = User.builder()
                .fullName("Inactive Student")
                .email("inactive@example.com")
                .whatsappNumber("+94770002")
                .school("Test School")
                .address("Test Address")
                .role(Role.STUDENT)
                .status(UserStatus.INACTIVE)
                .codeNumber("560002")
                .isVerified(true)
                .passwordHash(passwordEncoder.encode("password123"))
                .build();
        userRepository.save(inactiveStudent);

        User activeStudent2 = User.builder()
                .fullName("Active Student 2")
                .email("active2@example.com")
                .whatsappNumber("+94770003")
                .school("Test School")
                .address("Test Address")
                .role(Role.STUDENT)
                .status(UserStatus.ACTIVE)
                .codeNumber("560003")
                .isVerified(true)
                .passwordHash(passwordEncoder.encode("password123"))
                .build();
        userRepository.save(activeStudent2);

        // When & Then
        mockMvc.perform(get("/students")
                        .param("page", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.total", is(2)))
                .andExpect(jsonPath("$.items[*].fullName", containsInAnyOrder(
                        "Active Student 1",
                        "Active Student 2"
                )));
    }

    @Test
    @DisplayName("Should exclude unverified students")
    void shouldExcludeUnverifiedStudents() throws Exception {
        // Given - 2 verified students and 1 unverified student
        User verifiedStudent1 = User.builder()
                .fullName("Verified Student 1")
                .email("verified1@example.com")
                .whatsappNumber("+94770001")
                .school("Test School")
                .address("Test Address")
                .role(Role.STUDENT)
                .status(UserStatus.ACTIVE)
                .codeNumber("560001")
                .isVerified(true)
                .passwordHash(passwordEncoder.encode("password123"))
                .build();
        userRepository.save(verifiedStudent1);

        User unverifiedStudent = User.builder()
                .fullName("Unverified Student")
                .email("unverified@example.com")
                .whatsappNumber("+94770002")
                .school("Test School")
                .address("Test Address")
                .role(Role.STUDENT)
                .status(UserStatus.ACTIVE)
                .codeNumber("560002")
                .isVerified(false)
                .passwordHash(passwordEncoder.encode("password123"))
                .build();
        userRepository.save(unverifiedStudent);

        User verifiedStudent2 = User.builder()
                .fullName("Verified Student 2")
                .email("verified2@example.com")
                .whatsappNumber("+94770003")
                .school("Test School")
                .address("Test Address")
                .role(Role.STUDENT)
                .status(UserStatus.ACTIVE)
                .codeNumber("560003")
                .isVerified(true)
                .passwordHash(passwordEncoder.encode("password123"))
                .build();
        userRepository.save(verifiedStudent2);

        // When & Then
        mockMvc.perform(get("/students")
                        .param("page", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.total", is(2)))
                .andExpect(jsonPath("$.items[*].fullName", containsInAnyOrder(
                        "Verified Student 1",
                        "Verified Student 2"
                )));
    }

    @Test
    @DisplayName("Should exclude admins")
    void shouldExcludeAdmins() throws Exception {
        // Given - 2 students and 1 admin
        User student1 = User.builder()
                .fullName("Student 1")
                .email("student1@example.com")
                .whatsappNumber("+94770001")
                .school("Test School")
                .address("Test Address")
                .role(Role.STUDENT)
                .status(UserStatus.ACTIVE)
                .codeNumber("560001")
                .isVerified(true)
                .passwordHash(passwordEncoder.encode("password123"))
                .build();
        userRepository.save(student1);

        User admin = User.builder()
                .fullName("Admin User")
                .email("admin@example.com")
                .whatsappNumber("+94770002")
                .school("Admin School")
                .address("Admin Address")
                .role(Role.MAIN_ADMIN)
                .status(UserStatus.ACTIVE)
                .codeNumber("999999")
                .isVerified(true)
                .passwordHash(passwordEncoder.encode("password123"))
                .build();
        userRepository.save(admin);

        User student2 = User.builder()
                .fullName("Student 2")
                .email("student2@example.com")
                .whatsappNumber("+94770003")
                .school("Test School")
                .address("Test Address")
                .role(Role.STUDENT)
                .status(UserStatus.ACTIVE)
                .codeNumber("560002")
                .isVerified(true)
                .passwordHash(passwordEncoder.encode("password123"))
                .build();
        userRepository.save(student2);

        // When & Then
        mockMvc.perform(get("/students")
                        .param("page", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.total", is(2)))
                .andExpect(jsonPath("$.items[*].fullName", containsInAnyOrder("Student 1", "Student 2")))
                .andExpect(jsonPath("$.items[*].role", everyItem(is("STUDENT"))));
    }

    @Test
    @DisplayName("Should return empty items array when no active verified students exist")
    void shouldReturnEmptyItemsArrayWhenNoStudents() throws Exception {
        // Given - No students in database

        // When & Then
        mockMvc.perform(get("/students")
                        .param("page", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.total", is(0)));
    }

    @Test
    @DisplayName("Should return students ordered by creation date descending")
    void shouldReturnStudentsOrderedByCreatedAt() throws Exception {
        // Given - Create 3 students at different times
        User student1 = User.builder()
                .fullName("First Student")
                .email("first@example.com")
                .whatsappNumber("+94770001")
                .school("Test School")
                .address("Test Address")
                .role(Role.STUDENT)
                .status(UserStatus.ACTIVE)
                .codeNumber("560001")
                .isVerified(true)
                .passwordHash(passwordEncoder.encode("password123"))
                .build();
        userRepository.save(student1);

        Thread.sleep(100); // Small delay to ensure different timestamps

        User student2 = User.builder()
                .fullName("Second Student")
                .email("second@example.com")
                .whatsappNumber("+94770002")
                .school("Test School")
                .address("Test Address")
                .role(Role.STUDENT)
                .status(UserStatus.ACTIVE)
                .codeNumber("560002")
                .isVerified(true)
                .passwordHash(passwordEncoder.encode("password123"))
                .build();
        userRepository.save(student2);

        Thread.sleep(100);

        User student3 = User.builder()
                .fullName("Third Student")
                .email("third@example.com")
                .whatsappNumber("+94770003")
                .school("Test School")
                .address("Test Address")
                .role(Role.STUDENT)
                .status(UserStatus.ACTIVE)
                .codeNumber("560003")
                .isVerified(true)
                .passwordHash(passwordEncoder.encode("password123"))
                .build();
        userRepository.save(student3);

        // When & Then - Most recent first
        mockMvc.perform(get("/students")
                        .param("page", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(3)))
                .andExpect(jsonPath("$.items[0].fullName", is("Third Student")))
                .andExpect(jsonPath("$.items[1].fullName", is("Second Student")))
                .andExpect(jsonPath("$.items[2].fullName", is("First Student")));
    }

    @Test
    @DisplayName("Should handle pagination correctly - second page")
    void shouldHandleSecondPage() throws Exception {
        // Given - Create 5 students
        for (int i = 1; i <= 5; i++) {
            User student = User.builder()
                    .fullName("Student " + i)
                    .email("student" + i + "@example.com")
                    .whatsappNumber("+9477000" + i)
                    .school("Test School")
                    .address("Test Address")
                    .role(Role.STUDENT)
                    .status(UserStatus.ACTIVE)
                    .codeNumber("56000" + i)
                    .isVerified(true)
                    .passwordHash(passwordEncoder.encode("password123"))
                    .build();
            userRepository.save(student);
            Thread.sleep(10); // Ensure different timestamps
        }

        // When & Then - Request page 2 with pageSize 2
        mockMvc.perform(get("/students")
                        .param("page", "2")
                        .param("pageSize", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.total", is(5)));
    }

    @Test
    @DisplayName("Should return empty items when page exceeds available data")
    void shouldReturnEmptyItemsWhenPageExceedsData() throws Exception {
        // Given - Create 2 students
        for (int i = 1; i <= 2; i++) {
            User student = User.builder()
                    .fullName("Student " + i)
                    .email("student" + i + "@example.com")
                    .whatsappNumber("+9477000" + i)
                    .school("Test School")
                    .address("Test Address")
                    .role(Role.STUDENT)
                    .status(UserStatus.ACTIVE)
                    .codeNumber("56000" + i)
                    .isVerified(true)
                    .passwordHash(passwordEncoder.encode("password123"))
                    .build();
            userRepository.save(student);
        }

        // When & Then - Request page 3 (beyond available data)
        mockMvc.perform(get("/students")
                        .param("page", "3")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.total", is(2)));
    }
}
        // Given - 2 active students and 1 inactive student
        User activeStudent1 = User.builder()
                .fullName("Active Student 1")
                .email("active1@example.com")
                .whatsappNumber("+94770001")
                .school("Test School")
                .address("Test Address")
                .role(Role.STUDENT)
                .status(UserStatus.ACTIVE)
                .codeNumber("560001")
                .isVerified(true)
                .passwordHash(passwordEncoder.encode("password123"))
                .build();
        userRepository.save(activeStudent1);

        User inactiveStudent = User.builder()
                .fullName("Inactive Student")
                .email("inactive@example.com")
                .whatsappNumber("+94770002")
                .school("Test School")
                .address("Test Address")
                .role(Role.STUDENT)
                .status(UserStatus.INACTIVE)
                .codeNumber("560002")
                .isVerified(true)
                .passwordHash(passwordEncoder.encode("password123"))
                .build();
        userRepository.save(inactiveStudent);

        User activeStudent2 = User.builder()
                .fullName("Active Student 2")
                .email("active2@example.com")
                .whatsappNumber("+94770003")
                .school("Test School")
                .address("Test Address")
                .role(Role.STUDENT)
                .status(UserStatus.ACTIVE)
                .codeNumber("560003")
                .isVerified(true)
                .passwordHash(passwordEncoder.encode("password123"))
                .build();
        userRepository.save(activeStudent2);

        // When & Then
        mockMvc.perform(get("/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].fullName", hasItems(
                        "Active Student 1",
                        "Active Student 2"
                )))
                .andExpect(jsonPath("$[*].fullName", not(hasItem("Inactive Student"))));
    }

    @Test
    @DisplayName("Should exclude unverified students")
    void shouldExcludeUnverifiedStudents() throws Exception {
        // Given - 2 verified students and 1 unverified student
        User verifiedStudent1 = User.builder()
                .fullName("Verified Student 1")
                .email("verified1@example.com")
                .whatsappNumber("+94770001")
                .school("Test School")
                .address("Test Address")
                .role(Role.STUDENT)
                .status(UserStatus.ACTIVE)
                .codeNumber("560001")
                .isVerified(true)
                .passwordHash(passwordEncoder.encode("password123"))
                .build();
        userRepository.save(verifiedStudent1);

        User unverifiedStudent = User.builder()
                .fullName("Unverified Student")
                .email("unverified@example.com")
                .whatsappNumber("+94770002")
                .school("Test School")
                .address("Test Address")
                .role(Role.STUDENT)
                .status(UserStatus.ACTIVE)
                .codeNumber("560002")
                .isVerified(false)
                .passwordHash(passwordEncoder.encode("password123"))
                .build();
        userRepository.save(unverifiedStudent);

        User verifiedStudent2 = User.builder()
                .fullName("Verified Student 2")
                .email("verified2@example.com")
                .whatsappNumber("+94770003")
                .school("Test School")
                .address("Test Address")
                .role(Role.STUDENT)
                .status(UserStatus.ACTIVE)
                .codeNumber("560003")
                .isVerified(true)
                .passwordHash(passwordEncoder.encode("password123"))
                .build();
        userRepository.save(verifiedStudent2);

        // When & Then
        mockMvc.perform(get("/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].fullName", hasItems(
                        "Verified Student 1",
                        "Verified Student 2"
                )))
                .andExpect(jsonPath("$[*].fullName", not(hasItem("Unverified Student"))));
    }

    @Test
    @DisplayName("Should exclude admins")
    void shouldExcludeAdmins() throws Exception {
        // Given - 2 students and 1 admin
        User student1 = User.builder()
                .fullName("Student 1")
                .email("student1@example.com")
                .whatsappNumber("+94770001")
                .school("Test School")
                .address("Test Address")
                .role(Role.STUDENT)
                .status(UserStatus.ACTIVE)
                .codeNumber("560001")
                .isVerified(true)
                .passwordHash(passwordEncoder.encode("password123"))
                .build();
        userRepository.save(student1);

        User admin = User.builder()
                .fullName("Admin User")
                .email("admin@example.com")
                .whatsappNumber("+94770002")
                .school("Admin School")
                .address("Admin Address")
                .role(Role.MAIN_ADMIN)
                .status(UserStatus.ACTIVE)
                .codeNumber("999999")
                .isVerified(true)
                .passwordHash(passwordEncoder.encode("password123"))
                .build();
        userRepository.save(admin);

        User student2 = User.builder()
                .fullName("Student 2")
                .email("student2@example.com")
                .whatsappNumber("+94770003")
                .school("Test School")
                .address("Test Address")
                .role(Role.STUDENT)
                .status(UserStatus.ACTIVE)
                .codeNumber("560002")
                .isVerified(true)
                .passwordHash(passwordEncoder.encode("password123"))
                .build();
        userRepository.save(student2);

        // When & Then
        mockMvc.perform(get("/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].fullName", hasItems("Student 1", "Student 2")))
                .andExpect(jsonPath("$[*].fullName", not(hasItem("Admin User"))))
                .andExpect(jsonPath("$[*].role", everyItem(is("STUDENT"))));
    }

    @Test
    @DisplayName("Should return empty array when no active verified students exist")
    void shouldReturnEmptyArrayWhenNoStudents() throws Exception {
        // Given - No students in database

        // When & Then
        mockMvc.perform(get("/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Should return students ordered by creation date descending")
    void shouldReturnStudentsOrderedByCreatedAt() throws Exception {
        // Given - Create 3 students at different times
        User student1 = User.builder()
                .fullName("First Student")
                .email("first@example.com")
                .whatsappNumber("+94770001")
                .school("Test School")
                .address("Test Address")
                .role(Role.STUDENT)
                .status(UserStatus.ACTIVE)
                .codeNumber("560001")
                .isVerified(true)
                .passwordHash(passwordEncoder.encode("password123"))
                .build();
        userRepository.save(student1);

        Thread.sleep(100); // Small delay to ensure different timestamps

        User student2 = User.builder()
                .fullName("Second Student")
                .email("second@example.com")
                .whatsappNumber("+94770002")
                .school("Test School")
                .address("Test Address")
                .role(Role.STUDENT)
                .status(UserStatus.ACTIVE)
                .codeNumber("560002")
                .isVerified(true)
                .passwordHash(passwordEncoder.encode("password123"))
                .build();
        userRepository.save(student2);

        Thread.sleep(100);

        User student3 = User.builder()
                .fullName("Third Student")
                .email("third@example.com")
                .whatsappNumber("+94770003")
                .school("Test School")
                .address("Test Address")
                .role(Role.STUDENT)
                .status(UserStatus.ACTIVE)
                .codeNumber("560003")
                .isVerified(true)
                .passwordHash(passwordEncoder.encode("password123"))
                .build();
        userRepository.save(student3);

        // When & Then - Most recent first
        mockMvc.perform(get("/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].fullName", is("Third Student")))
                .andExpect(jsonPath("$[1].fullName", is("Second Student")))
                .andExpect(jsonPath("$[2].fullName", is("First Student")));
    }
}

