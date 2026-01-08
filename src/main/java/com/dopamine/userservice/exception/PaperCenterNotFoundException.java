package com.dopamine.userservice.exception;
/**
 * Exception thrown when a paper center is not found.
 */
public class PaperCenterNotFoundException extends RuntimeException {
    public PaperCenterNotFoundException(String message) {
        super(message);
    }
}
