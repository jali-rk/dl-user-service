package com.dopamine.userservice.service;

import java.util.UUID;

/**
 * Service for sending email notifications via the BFF.
 */
public interface EmailNotificationService {

    /**
     * Send a verification code email to a student (initial registration).
     *
     * @param userId recipient user ID
     * @param code verification code (6-digit student code)
     */
    void sendVerificationCodeEmail(UUID userId, String code);

    /**
     * Send a resend verification code email to a student.
     *
     * @param userId recipient user ID
     * @param code new verification code (6-digit student code)
     */
    void sendResendVerificationCodeEmail(UUID userId, String code);
}
