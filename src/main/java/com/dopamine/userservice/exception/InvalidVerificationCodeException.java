package com.dopamine.userservice.exception;

/**
 * Exception thrown when a verification code is invalid or expired.
 */
public class InvalidVerificationCodeException extends RuntimeException {

    public InvalidVerificationCodeException(String message) {
        super(message);
    }
}

