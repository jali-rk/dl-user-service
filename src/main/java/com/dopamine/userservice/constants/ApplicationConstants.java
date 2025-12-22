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
         * Expiry duration for registration verification codes (3 minutes)
         */
        public static final Duration REGISTRATION_CODE_EXPIRY = Duration.ofMinutes(3);
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
}

