package com.dopamine.userservice.constants;

import java.time.Duration;

/**
 * Application-wide constants.
 */
public final class ApplicationConstants {

    private ApplicationConstants() {
        // Private constructor to prevent instantiation
    }

    /**
     * Verification code constants
     */
    public static final class VerificationCode {
        private VerificationCode() {}

        /**
         * Expiry duration for registration verification codes (5 minutes)
         */
        public static final Duration REGISTRATION_CODE_EXPIRY = Duration.ofMinutes(5);
    }

    /**
     * Password reset constants
     */
    public static final class PasswordReset {
        private PasswordReset() {}

        /**
         * Expiry duration for password reset tokens (30 minutes)
         */
        public static final Duration PASSWORD_RESET_TOKEN_EXPIRY = Duration.ofMinutes(30);
    }

    /**
     * Email notification constants
     */
    public static final class Email {
        private Email() {}

        // BFF broadcast notification endpoint
        public static final String BFF_NOTIFICATION_ENDPOINT = "/admin/notifications/broadcast";

        // Email subjects/titles
        public static final String SUBJECT_VERIFICATION_CODE = "Test Email";
        public static final String SUBJECT_PASSWORD_RESET = "Reset Your DopamineLite Password";
        public static final String SUBJECT_RESEND_VERIFICATION_CODE = "Your New DopamineLite Verification Code";

        // Email template names (deprecated - using direct body now)
        public static final String TEMPLATE_VERIFICATION_CODE = "verification-code";
        public static final String TEMPLATE_PASSWORD_RESET = "password-reset";
        public static final String TEMPLATE_RESEND_VERIFICATION_CODE = "resend-verification-code";

        // Template variable keys (deprecated - using direct body now)
        public static final String VAR_CODE = "code";
        public static final String VAR_TOKEN = "token";

        // Email body content - includes verification code
        public static final String BODY_VERIFICATION_CODE = """
                Welcome to DopamineLite!
                
                Your verification code is (ඔබගේ සත්‍යාපන කේතය): %s
                
                This code will expire in 5 minutes.
                
                If you didn't request this code, please ignore this email.
                
                Best regards,
                DopamineLite Team
                """;

        public static final String BODY_PASSWORD_RESET = """
                You have requested to reset your password.
                
                Your password reset token is: %s
                
                This token will expire in 30 minutes.
                
                If you didn't request this, please ignore this email.
                
                Best regards,
                DopamineLite Team
                """;

        public static final String BODY_RESEND_VERIFICATION_CODE = """
                You have requested a new verification code.
                
                Your new verification code is (ඔබගේ නව සත්‍යාපන කේතය): %s
                
                This code will expire in 5 minutes.
                
                Note: This new code replaces your previous code.
                
                Best regards,
                DopamineLite Team
                """;
    }
}

