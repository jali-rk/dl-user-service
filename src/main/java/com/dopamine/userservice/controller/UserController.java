package com.dopamine.userservice.controller;

import com.dopamine.userservice.dto.UserPublicView;
import com.dopamine.userservice.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller for generic user operations.
 * Provides endpoints for user lookup by ID or email.
 */
@RestController
@RequestMapping("/users")
@Slf4j
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Get user by ID (any role).
     * GET /users/{userId}
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserPublicView> getUserById(@PathVariable UUID userId) {
        log.debug("Get user request for ID: {}", userId);
        UserPublicView user = userService.getUserById(userId);
        return ResponseEntity.ok(user);
    }

    /**
     * Get user by email.
     * GET /users/by-email?email={email}
     */
    @GetMapping("/by-email")
    public ResponseEntity<UserPublicView> getUserByEmail(@RequestParam String email) {
        log.debug("Get user request for email: {}", email);
        UserPublicView user = userService.getUserByEmail(email);
        return ResponseEntity.ok(user);
    }
}

