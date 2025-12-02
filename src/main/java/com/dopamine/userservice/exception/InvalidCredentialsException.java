package com.dopamine.userservice.exception;

/**
 * Exception thrown when user credentials are invalid.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}

