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
 * Integration tests for GET /students endpoint.
 */
@AutoConfigureMockMvc
@Transactional
@DisplayName("Get All Active Verified Students Integration Tests")
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
    @DisplayName("Should return all active and verified students")
    void shouldReturnAllActiveVerifiedStudents() throws Exception {
        // Given - Create 3 active and verified students
        for (int i = 1; i <= 3; i++) {
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

        // When & Then
        mockMvc.perform(get("/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].fullName", hasItems(
                        "Active Student 1",
                        "Active Student 2",
                        "Active Student 3"
                )))
                .andExpect(jsonPath("$[*].status", everyItem(is("ACTIVE"))))
                .andExpect(jsonPath("$[*].isVerified", everyItem(is(true))))
                .andExpect(jsonPath("$[*].role", everyItem(is("STUDENT"))));
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

