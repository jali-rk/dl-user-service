package com.dopamine.userservice.exception;

/**
 * Exception thrown when attempting to change/set an email that is already used by another account.
 */
public class EmailAlreadyInUseException extends RuntimeException {

    public EmailAlreadyInUseException(String message) {
        super(message);
    }
}

