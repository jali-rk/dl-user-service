package com.dopamine.userservice.constants;

/**
 * Enum for email notification types.
 * Values match exactly what the BFF notification service expects.
 */
public enum EmailType {
    /**
     * Payment submission status changed (approved/rejected).
     */
    PAYMENT_STATUS_CHANGED,

    /**
     * Issue status changed (open/in-progress/solved).
     */
    ISSUE_STATUS_CHANGED,

    /**
     * New message in an issue conversation.
     */
    ISSUE_MESSAGE_NEW,

    /**
     * Student account verified successfully.
     */
    STUDENT_VERIFIED,

    /**
     * New student registered (verification code sent).
     */
    STUDENT_REGISTERED,

    /**
     * Admin broadcast message to students.
     */
    ADMIN_BROADCAST
}

