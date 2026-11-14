package com.dopamine.userservice.exception;

/**
 * Exception thrown when attempting to register or set a NIC that is already used by another account.
 */
public class NicAlreadyInUseException extends RuntimeException {

    public NicAlreadyInUseException(String message) {
        super(message);
    }
}
