package com.dopamine.userservice.controller;

import com.dopamine.userservice.dto.InstructorUpdateRequest;
import com.dopamine.userservice.dto.UserPublicView;
import com.dopamine.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller for instructor-related operations.
 * Handles instructor retrieval, updates, and deletion.
 * Instructor accounts are created via {@link AdminController#createAdmin} (POST /admins with role INSTRUCTOR).
 */
@RestController
@RequestMapping("/instructors")
@Slf4j
public class InstructorController {

    private final UserService userService;

    public InstructorController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Get instructor by ID.
     * GET /instructors/{instructorId}
     */
    @GetMapping("/{instructorId}")
    public ResponseEntity<UserPublicView> getInstructor(@PathVariable UUID instructorId) {
        log.debug("Get instructor request for ID: {}", instructorId);
        UserPublicView instructor = userService.getInstructorById(instructorId);
        return ResponseEntity.ok(instructor);
    }

    /**
     * Update instructor profile.
     * PATCH /instructors/{instructorId}
     */
    @PatchMapping("/{instructorId}")
    public ResponseEntity<UserPublicView> updateInstructor(
            @PathVariable UUID instructorId,
            @Valid @RequestBody InstructorUpdateRequest request) {
        log.info("Update instructor request for ID: {}", instructorId);
        UserPublicView updatedInstructor = userService.updateInstructor(instructorId, request);
        return ResponseEntity.ok(updatedInstructor);
    }

    /**
     * Delete (soft delete) an instructor.
     * DELETE /instructors/{instructorId}
     */
    @DeleteMapping("/{instructorId}")
    public ResponseEntity<Void> deleteInstructor(@PathVariable UUID instructorId) {
        log.info("Delete instructor request for ID: {}", instructorId);
        userService.deleteInstructor(instructorId);
        return ResponseEntity.noContent().build();
    }
}
