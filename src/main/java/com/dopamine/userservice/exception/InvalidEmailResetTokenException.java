package com.dopamine.userservice.exception;

/**
 * Exception thrown when an email reset token is invalid or expired.
 */
public class InvalidEmailResetTokenException extends RuntimeException {

    public InvalidEmailResetTokenException(String message) {
        super(message);
    }
}

