package com.dopamine.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO for student list item in paginated response.
 * Contains minimal fields needed for student listing.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentListItem {

    /**
     * User unique identifier.
     */
    private String id;

    /**
     * User's full name.
     */
    private String fullName;

    /**
     * User's email address.
     */
    private String email;

    /**
     * User's WhatsApp number.
     */
    private String whatsappNumber;

    /**
     * User's 6-digit code number.
     */
    private String codeNumber;

    /**
     * User role (always STUDENT for this endpoint).
     */
    private String role;

    /**
     * Whether the student is verified.
     */
    private Boolean isVerified;

    /**
     * Registration number (numeric part of code number).
     */
    private Integer registrationNumber;

    /**
     * Account creation timestamp.
     */
    private Instant createdAt;

    /**
     * National Identity Card number (NIC).
     */
    private String nic;

    /**
     * School name.
     */
    private String school;

    /**
     * Address.
     */
    private String address;
}
