package com.dopamine.userservice.controller;

import com.dopamine.userservice.dto.LoginHistoryRequest;
import com.dopamine.userservice.service.LoginHistoryService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Internal endpoint the BFF calls after a successful login.
 */
@RestController
@RequestMapping("/internal/auth/login-history")
@Slf4j
public class LoginHistoryController {

    private final LoginHistoryService loginHistoryService;

    public LoginHistoryController(LoginHistoryService loginHistoryService) {
        this.loginHistoryService = loginHistoryService;
    }

    /**
     * Record a successful login.
     * POST /internal/auth/login-history
     */
    @PostMapping
    public ResponseEntity<Void> recordLogin(@Valid @RequestBody LoginHistoryRequest request) {
        log.info("Login history request for userId: {}", request.getUserId());
        loginHistoryService.recordLogin(request);
        return ResponseEntity.status(201).build();
    }
}
