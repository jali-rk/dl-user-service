package com.dopamine.userservice.controller;

import com.dopamine.userservice.domain.Role;
import com.dopamine.userservice.domain.UserStatus;
import com.dopamine.userservice.dto.AdminCreateRequest;
import com.dopamine.userservice.dto.AdminUpdateRequest;
import com.dopamine.userservice.dto.UserPublicView;
import com.dopamine.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for admin-related operations.
 * Handles admin creation, retrieval, and updates.
 */
@RestController
@RequestMapping("/admins")
@Slf4j
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Create a new admin.
     * POST /admins
     */
    @PostMapping
    public ResponseEntity<UserPublicView> createAdmin(
            @Valid @RequestBody AdminCreateRequest request) {
        log.info("Admin creation request received for email: {} with role: {}",
                request.getEmail(), request.getRole());
        UserPublicView admin = userService.createAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(admin);
    }

    /**
     * Get admin by ID.
     * GET /admins/{adminId}
     */
    @GetMapping("/{adminId}")
    public ResponseEntity<UserPublicView> getAdmin(@PathVariable UUID adminId) {
        log.debug("Get admin request for ID: {}", adminId);
        UserPublicView admin = userService.getAdminById(adminId);
        return ResponseEntity.ok(admin);
    }

    /**
     * Update admin profile.
     * PATCH /admins/{adminId}
     */
    @PatchMapping("/{adminId}")
    public ResponseEntity<UserPublicView> updateAdmin(
            @PathVariable UUID adminId,
            @Valid @RequestBody AdminUpdateRequest request) {
        log.info("Update admin request for ID: {}", adminId);
        UserPublicView updatedAdmin = userService.updateAdmin(adminId, request);
        return ResponseEntity.ok(updatedAdmin);
    }

    /**
     * List admins by role.
     * GET /admins?role={role}
     */
    @GetMapping
    public ResponseEntity<List<UserPublicView>> listAdmins(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) UserStatus status) {

        if (role != null && status != null) {
            log.debug("Listing admins by role: {} and status: {}", role, status);
            List<UserPublicView> admins = userService.listAdminsByRoleAndStatus(role, status);
            return ResponseEntity.ok(admins);
        } else if (role != null) {
            log.debug("Listing admins by role: {}", role);
            List<UserPublicView> admins = userService.listAdminsByRole(role);
            return ResponseEntity.ok(admins);
        } else {
            // Default to listing all admins (ADMIN and MAIN_ADMIN roles)
            log.debug("Listing all admins with roles ADMIN and MAIN_ADMIN");
            List<UserPublicView> allAdmins = userService.listAdminsByRole(Role.ADMIN);
            allAdmins.addAll(userService.listAdminsByRole(Role.MAIN_ADMIN));
            log.debug("Total admins found: {}", allAdmins.size());
            return ResponseEntity.ok(allAdmins);
        }

    }

    /**
     * Delete (soft delete) an admin.
     * DELETE /admins/{adminId}
     */
    @DeleteMapping("/{adminId}")
    public ResponseEntity<Void> deleteAdmin(@PathVariable UUID adminId) {
        log.info("Delete admin request for ID: {}", adminId);
        userService.deleteAdmin(adminId);
        return ResponseEntity.noContent().build();
    }
}
