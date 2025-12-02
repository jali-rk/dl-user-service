package com.dopamine.userservice.domain;

/**
 * Enum representing the status of a user account.
 */
public enum UserStatus {
    /**
     * User account is active and can be used
     */
    ACTIVE,

    /**
     * User account is inactive (temporarily disabled)
     */
    INACTIVE,

    /**
     * User account is blocked (cannot be used)
     */
    BLOCKED
}

