package com.dopamine.userservice.service;

import com.dopamine.userservice.domain.LoginHistory;
import com.dopamine.userservice.dto.LoginHistoryRequest;
import com.dopamine.userservice.exception.UserNotFoundException;
import com.dopamine.userservice.repository.LoginHistoryRepository;
import com.dopamine.userservice.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records successful logins reported by the BFF.
 */
@Service
@Slf4j
public class LoginHistoryService {

    private final LoginHistoryRepository loginHistoryRepository;
    private final UserRepository userRepository;

    public LoginHistoryService(LoginHistoryRepository loginHistoryRepository, UserRepository userRepository) {
        this.loginHistoryRepository = loginHistoryRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void recordLogin(LoginHistoryRequest request) {
        if (!userRepository.existsById(request.getUserId())) {
            throw new UserNotFoundException("User not found with id: " + request.getUserId());
        }

        loginHistoryRepository.save(LoginHistory.builder()
                .userId(request.getUserId())
                .ipAddress(request.getIpAddress())
                .userAgent(request.getUserAgent())
                .build());

        log.info("Recorded login for user: {}", request.getUserId());
    }
}
