package com.dopamine.userservice.controller;

import com.dopamine.userservice.dto.*;
import com.dopamine.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for internal authentication operations.
 * These endpoints are called only by the BFF for credential validation and password reset.
 */
@RestController
@RequestMapping("/internal/auth")
@Slf4j
public class InternalAuthController {

    private final UserService userService;

    public InternalAuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Validate user credentials for login.
     * POST /internal/auth/validate-credentials
     */
    @PostMapping("/validate-credentials")
    public ResponseEntity<CredentialsValidationResponse> validateCredentials(
            @Valid @RequestBody CredentialsValidationRequest request) {
        log.info("Credential validation request for email: {}", request.getEmail());
        CredentialsValidationResponse response = userService.validateCredentials(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Request password reset (generates token and returns it for BFF to send via email).
     * POST /internal/auth/password-reset/request
     */
    @PostMapping("/password-reset/request")
    public ResponseEntity<PasswordResetResponse> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request) {
        log.info("Password reset request for email: {}", request.getEmail());
        PasswordResetResponse response = userService.requestPasswordReset(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Confirm password reset with token.
     * POST /internal/auth/password-reset/confirm
     */
    @PostMapping("/password-reset/confirm")
    public ResponseEntity<Void> confirmPasswordReset(
            @Valid @RequestBody PasswordResetConfirmRequest request) {
        log.info("Password reset confirmation request");
        userService.confirmPasswordReset(request);
        return ResponseEntity.ok().build();
    }

    /**
     * Request email reset (generates token and returns it for BFF to send via Notification Service).
     * POST /internal/auth/email-reset/request
     */
    @PostMapping("/email-reset/request")
    public ResponseEntity<EmailResetResponse> requestEmailReset(
            @Valid @RequestBody EmailResetRequest request) {
        log.info("Email reset request for userId: {}", request.getUserId());
        EmailResetResponse response = userService.requestEmailReset(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Confirm email reset with token.
     * POST /internal/auth/email-reset/confirm
     */
    @PostMapping("/email-reset/confirm")
    public ResponseEntity<EmailResetConfirmResponse> confirmEmailReset(
            @Valid @RequestBody EmailResetConfirmRequest request) {
        log.info("Email reset confirmation request");
        EmailResetConfirmResponse response = userService.confirmEmailReset(request);
        return ResponseEntity.ok(response);
    }
}
