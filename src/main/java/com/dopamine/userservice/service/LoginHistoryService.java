package com.dopamine.userservice.service;

import com.dopamine.userservice.domain.LoginHistory;
import com.dopamine.userservice.dto.LoginHistoryRequest;
import com.dopamine.userservice.exception.UserNotFoundException;
import com.dopamine.userservice.repository.LoginHistoryRepository;
import com.dopamine.userservice.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * Records successful logins reported by the BFF and purges entries past the retention period.
 */
@Service
@Slf4j
public class LoginHistoryService {

    private final LoginHistoryRepository loginHistoryRepository;
    private final UserRepository userRepository;
    private final int retentionDays;

    public LoginHistoryService(LoginHistoryRepository loginHistoryRepository,
                               UserRepository userRepository,
                               @Value("${login-history.retention-days:90}") int retentionDays) {
        this.loginHistoryRepository = loginHistoryRepository;
        this.userRepository = userRepository;
        this.retentionDays = retentionDays;
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

    /**
     * Deletes login history older than the retention period. Runs daily at 03:00 Sri Lanka time.
     * Safe to run on several instances at once: the delete is idempotent.
     */
    @Scheduled(cron = "${login-history.purge-cron:0 0 3 * * *}", zone = "Asia/Colombo")
    @Transactional
    public void purgeExpired() {
        Instant cutoff = Instant.now().minus(Duration.ofDays(retentionDays));
        int deleted = loginHistoryRepository.deleteOlderThan(cutoff);
        log.info("Purged {} login history entries older than {} days", deleted, retentionDays);
    }
}
