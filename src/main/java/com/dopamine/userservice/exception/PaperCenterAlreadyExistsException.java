package com.dopamine.userservice.exception;
/**
 * Exception thrown when attempting to create a paper center that already exists.
 */
public class PaperCenterAlreadyExistsException extends RuntimeException {
    public PaperCenterAlreadyExistsException(String message) {
        super(message);
    }
}
