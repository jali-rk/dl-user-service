package com.dopamine.userservice.controller;

import com.dopamine.userservice.base.BaseIntegrationTest;
import com.dopamine.userservice.domain.Role;
import com.dopamine.userservice.domain.User;
import com.dopamine.userservice.domain.UserStatus;
import com.dopamine.userservice.dto.BatchUserRequest;
import com.dopamine.userservice.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for UserController batch endpoint.
 * Tests filtering by status = ACTIVE and isVerified = true.
 */
@AutoConfigureMockMvc
@Transactional
class UserControllerBatchIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    private List<User> activeVerifiedUsers;
    private User inactiveUser;
    private User unverifiedUser;

    @BeforeEach
    void setUp() {
        // Create ACTIVE and VERIFIED users (should be returned)
        activeVerifiedUsers = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            User user = User.builder()
                    .fullName("Test User " + i)
                    .email("test" + i + "@example.com")
                    .whatsappNumber("+123456789" + i)
                    .school("Test School " + i)
                    .address("Test Address " + i)
                    .role(Role.STUDENT)
                    .status(UserStatus.ACTIVE)
                    .codeNumber("50000" + i)
                    .isVerified(true)
                    .passwordHash(passwordEncoder.encode("password123"))
                    .build();
            activeVerifiedUsers.add(userRepository.save(user));
        }

        // Create INACTIVE user (should NOT be returned)
        inactiveUser = User.builder()
                .fullName("Inactive User")
                .email("inactive@example.com")
                .whatsappNumber("+1234567890")
                .school("Test School")
                .address("Test Address")
                .role(Role.STUDENT)
                .status(UserStatus.INACTIVE)
                .codeNumber("500010")
                .isVerified(true)
                .passwordHash(passwordEncoder.encode("password123"))
                .build();
        inactiveUser = userRepository.save(inactiveUser);

        // Create unverified user (should NOT be returned)
        unverifiedUser = User.builder()
                .fullName("Unverified User")
                .email("unverified@example.com")
                .whatsappNumber("+1234567891")
                .school("Test School")
                .address("Test Address")
                .role(Role.STUDENT)
                .status(UserStatus.ACTIVE)
                .codeNumber("500011")
                .isVerified(false)
                .passwordHash(passwordEncoder.encode("password123"))
                .build();
        unverifiedUser = userRepository.save(unverifiedUser);
    }

    @Test
    void testBatchUserPublicData_success() throws Exception {
        // Prepare request with 3 ACTIVE+verified user IDs
        List<UUID> userIds = List.of(
                activeVerifiedUsers.get(0).getId(),
                activeVerifiedUsers.get(1).getId(),
                activeVerifiedUsers.get(2).getId()
        );

        BatchUserRequest request = BatchUserRequest.builder()
                .userIds(userIds)
                .build();

        mockMvc.perform(post("/users/batch/public")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].id", containsInAnyOrder(
                        activeVerifiedUsers.get(0).getId().toString(),
                        activeVerifiedUsers.get(1).getId().toString(),
                        activeVerifiedUsers.get(2).getId().toString()
                )))
                // Verify only minimal fields are present
                .andExpect(jsonPath("$[0].id", notNullValue()))
                .andExpect(jsonPath("$[0].fullName", notNullValue()))
                .andExpect(jsonPath("$[0].whatsappNumber", notNullValue()))
                .andExpect(jsonPath("$[0].email", notNullValue()))
                .andExpect(jsonPath("$[0].codeNumber", notNullValue()))
                // Verify extended fields are NOT present
                .andExpect(jsonPath("$[0].school").doesNotExist())
                .andExpect(jsonPath("$[0].address").doesNotExist())
                .andExpect(jsonPath("$[0].role").doesNotExist())
                .andExpect(jsonPath("$[0].status").doesNotExist());
    }

    @Test
    void testBatchUserPublicData_filtersInactiveUsers() throws Exception {
        // Request includes ACTIVE and INACTIVE users
        List<UUID> userIds = List.of(
                activeVerifiedUsers.get(0).getId(),
                inactiveUser.getId(),  // Should be filtered out
                activeVerifiedUsers.get(1).getId()
        );

        BatchUserRequest request = BatchUserRequest.builder()
                .userIds(userIds)
                .build();

        mockMvc.perform(post("/users/batch/public")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2))) // Only 2 ACTIVE users
                .andExpect(jsonPath("$[*].id", containsInAnyOrder(
                        activeVerifiedUsers.get(0).getId().toString(),
                        activeVerifiedUsers.get(1).getId().toString()
                )))
                .andExpect(jsonPath("$[*].id", not(contains(inactiveUser.getId().toString()))));
    }

    @Test
    void testBatchUserPublicData_filtersUnverifiedUsers() throws Exception {
        // Request includes verified and unverified users
        List<UUID> userIds = List.of(
                activeVerifiedUsers.get(0).getId(),
                unverifiedUser.getId(),  // Should be filtered out
                activeVerifiedUsers.get(1).getId()
        );

        BatchUserRequest request = BatchUserRequest.builder()
                .userIds(userIds)
                .build();

        mockMvc.perform(post("/users/batch/public")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2))) // Only 2 verified users
                .andExpect(jsonPath("$[*].id", containsInAnyOrder(
                        activeVerifiedUsers.get(0).getId().toString(),
                        activeVerifiedUsers.get(1).getId().toString()
                )))
                .andExpect(jsonPath("$[*].id", not(contains(unverifiedUser.getId().toString()))));
    }

    @Test
    void testBatchUserPublicData_withNonExistentIds() throws Exception {
        // Mix of existing and non-existent IDs
        List<UUID> userIds = List.of(
                activeVerifiedUsers.get(0).getId(),
                UUID.randomUUID(), // Non-existent
                activeVerifiedUsers.get(1).getId(),
                UUID.randomUUID()  // Non-existent
        );

        BatchUserRequest request = BatchUserRequest.builder()
                .userIds(userIds)
                .build();

        mockMvc.perform(post("/users/batch/public")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2))) // Only 2 existing users
                .andExpect(jsonPath("$[*].id", containsInAnyOrder(
                        activeVerifiedUsers.get(0).getId().toString(),
                        activeVerifiedUsers.get(1).getId().toString()
                )));
    }

    @Test
    void testBatchUserPublicData_emptyList() throws Exception {
        BatchUserRequest request = BatchUserRequest.builder()
                .userIds(List.of())
                .build();

        mockMvc.perform(post("/users/batch/public")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()); // Validation error: min size 1
    }

    @Test
    void testBatchUserPublicData_nullUserIds() throws Exception {
        String requestJson = "{\"userIds\": null}";

        mockMvc.perform(post("/users/batch/public")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest()); // Validation error: not null
    }

    @Test
    void testBatchUserPublicData_tooManyIds() throws Exception {
        // Create request with 1001 IDs (exceeds max of 1000)
        List<UUID> userIds = new ArrayList<>();
        for (int i = 0; i < 1001; i++) {
            userIds.add(UUID.randomUUID());
        }

        BatchUserRequest request = BatchUserRequest.builder()
                .userIds(userIds)
                .build();

        mockMvc.perform(post("/users/batch/public")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("BATCH_SIZE_EXCEEDED")))
                .andExpect(jsonPath("$.message", is("Maximum 1000 user IDs allowed per request")));
    }

    @Test
    void testBatchUserPublicData_exactlyMaxAllowed() throws Exception {
        // Create request with exactly 1000 IDs (should succeed)
        List<UUID> userIds = new ArrayList<>();
        // Add our 3 active verified test users
        activeVerifiedUsers.forEach(user -> userIds.add(user.getId()));
        // Fill rest with non-existent IDs to reach 1000
        for (int i = 3; i < 1000; i++) {
            userIds.add(UUID.randomUUID());
        }

        BatchUserRequest request = BatchUserRequest.builder()
                .userIds(userIds)
                .build();

        mockMvc.perform(post("/users/batch/public")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3))); // Only 3 existing ACTIVE+verified users
    }

    @Test
    void testBatchUserPublicData_singleUser() throws Exception {
        List<UUID> userIds = List.of(activeVerifiedUsers.get(0).getId());

        BatchUserRequest request = BatchUserRequest.builder()
                .userIds(userIds)
                .build();

        mockMvc.perform(post("/users/batch/public")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(activeVerifiedUsers.get(0).getId().toString())))
                .andExpect(jsonPath("$[0].fullName", is(activeVerifiedUsers.get(0).getFullName())))
                .andExpect(jsonPath("$[0].email", is(activeVerifiedUsers.get(0).getEmail())))
                .andExpect(jsonPath("$[0].whatsappNumber", is(activeVerifiedUsers.get(0).getWhatsappNumber())))
                .andExpect(jsonPath("$[0].codeNumber", is(activeVerifiedUsers.get(0).getCodeNumber())));
    }
}

