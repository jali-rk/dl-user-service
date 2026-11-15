package com.dopamine.userservice.controller;

import com.dopamine.userservice.base.BaseIntegrationTest;
import com.dopamine.userservice.domain.LoginHistory;
import com.dopamine.userservice.domain.User;
import com.dopamine.userservice.dto.LoginHistoryRequest;
import com.dopamine.userservice.repository.LoginHistoryRepository;
import com.dopamine.userservice.repository.UserRepository;
import com.dopamine.userservice.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Login History Controller Integration Tests")
class LoginHistoryControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LoginHistoryRepository loginHistoryRepository;

    @Value("${user.service.internal-token}")
    private String serviceToken;

    @BeforeEach
    void setUp() {
        loginHistoryRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should record login history for existing user")
    void shouldRecordLoginHistory() throws Exception {
        User user = userRepository.save(TestDataBuilder.verifiedStudent().email("history@example.com").build());

        LoginHistoryRequest request = LoginHistoryRequest.builder()
                .userId(user.getId())
                .ipAddress("203.0.113.7")
                .userAgent("Mozilla/5.0 (test)")
                .build();

        mockMvc.perform(post("/internal/auth/login-history")
                .header("X-Service-Token", serviceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
                .andExpect(status().isCreated());

        List<LoginHistory> entries = loginHistoryRepository.findByUserIdOrderByLoggedInAtDesc(user.getId());
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getIpAddress()).isEqualTo("203.0.113.7");
        assertThat(entries.get(0).getUserAgent()).isEqualTo("Mozilla/5.0 (test)");
        assertThat(entries.get(0).getLoggedInAt()).isNotNull();
    }

    @Test
    @DisplayName("Should return 404 for unknown user")
    void shouldReturnNotFoundForUnknownUser() throws Exception {
        LoginHistoryRequest request = LoginHistoryRequest.builder()
                .userId(UUID.randomUUID())
                .ipAddress("203.0.113.7")
                .build();

        mockMvc.perform(post("/internal/auth/login-history")
                .header("X-Service-Token", serviceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
                .andExpect(status().isNotFound());

        assertThat(loginHistoryRepository.count()).isZero();
    }

    @Test
    @DisplayName("Should reject request without service token")
    void shouldRejectWithoutServiceToken() throws Exception {
        LoginHistoryRequest request = LoginHistoryRequest.builder().userId(UUID.randomUUID()).build();

        mockMvc.perform(post("/internal/auth/login-history")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
                .andExpect(status().isUnauthorized());
    }
}
