package com.dopamine.userservice.exception;

/**
 * Exception thrown when a user is not verified but attempts to log in.
 */
public class UnverifiedUserException extends RuntimeException {

    public UnverifiedUserException(String message) {
        super(message);
    }
}

