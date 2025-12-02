package com.dopamine.userservice.exception;

/**
 * Exception thrown when a password reset token is invalid or expired.
 */
public class InvalidPasswordResetTokenException extends RuntimeException {

    public InvalidPasswordResetTokenException(String message) {
        super(message);
    }
}

