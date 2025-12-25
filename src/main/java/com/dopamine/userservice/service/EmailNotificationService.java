package com.dopamine.userservice.service;

/**
 * Service for sending email notifications via BFF.
 * Uses only approved email types: STUDENT_REGISTERED, STUDENT_VERIFIED, etc.
 */
public interface EmailNotificationService {

    /**
     * Send a verification code email to a student (initial registration).
     *
     * @param email recipient email
     * @param code verification code (6-digit student code)
     */
    void sendVerificationCodeEmail(String email, String code);


    /**
     * Send a resend verification code email to a student.
     *
     * @param email recipient email
     * @param code new verification code (6-digit student code)
     */
    void sendResendVerificationCodeEmail(String email, String code);
}

