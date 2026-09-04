package com.dopamine.userservice.domain;

/**
 * Enum representing the role of a user in the system.
 */
public enum Role {
    /**
     * Student role - for students enrolled in the platform
     */
    STUDENT,

    /**
     * Admin role - for regular administrators
     */
    ADMIN,

    /**
     * Main admin role - for super administrators with full privileges
     */
    MAIN_ADMIN,

    /**
     * Instructor role - paper-center staff who scan student QR passes to mark attendance
     */
    INSTRUCTOR
}


