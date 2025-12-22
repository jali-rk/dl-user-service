package com.dopamine.userservice.controller;

import com.dopamine.userservice.dto.*;
import com.dopamine.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller for student-related operations.
 * Handles student registration, verification, and profile management.
 */
@RestController
@RequestMapping("/students")
@Slf4j
public class StudentController {

    private final UserService userService;

    public StudentController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Register a new student.
     * POST /students/registrations
     */
    @PostMapping("/registrations")
    public ResponseEntity<StudentRegistrationResponse> registerStudent(
            @Valid @RequestBody StudentRegistrationRequest request) {
        log.info("Student registration request received for email: {}", request.getEmail());
        StudentRegistrationResponse response = userService.registerStudent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Verify student registration code.
     * POST /students/verify-code
     */
    @PostMapping("/verify-code")
    public ResponseEntity<UserPublicView> verifyStudentCode(
            @Valid @RequestBody VerifyCodeRequest request) {
        log.info("Student verification request received for email: {}", request.getEmail());
        log.info("Code: {}", request.getCode());
        UserPublicView user = userService.verifyStudentCode(request);
        return ResponseEntity.ok(user);
    }

    /**
     * Resend verification code to student.
     * POST /students/resend-verification-code
     */
    @PostMapping("/resend-verification-code")
    public ResponseEntity<ResendVerificationCodeResponse> resendVerificationCode(
            @Valid @RequestBody ResendVerificationCodeRequest request) {
        log.info("Resend verification code request received for email: {}", request.getEmail());
        ResendVerificationCodeResponse response = userService.resendVerificationCode(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get student by ID.
     * GET /students/{studentId}
     */
    @GetMapping("/{studentId}")
    public ResponseEntity<UserPublicView> getStudent(@PathVariable UUID studentId) {
        log.debug("Get student request for ID: {}", studentId);
        UserPublicView student = userService.getStudentById(studentId);
        return ResponseEntity.ok(student);
    }

    /**
     * Update student profile.
     * PATCH /students/{studentId}
     */
    @PatchMapping("/{studentId}")
    public ResponseEntity<UserPublicView> updateStudent(
            @PathVariable UUID studentId,
            @Valid @RequestBody StudentUpdateRequest request) {
        log.info("Update student request for ID: {}", studentId);
        UserPublicView updatedStudent = userService.updateStudent(studentId, request);
        return ResponseEntity.ok(updatedStudent);
    }
}

