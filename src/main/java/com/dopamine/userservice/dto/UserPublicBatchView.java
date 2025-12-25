package com.dopamine.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Response DTO for batch user public data endpoint.
 * Contains only minimal public information as specified by BFF requirements.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPublicBatchView {

    /**
     * User unique identifier.
     */
    private UUID id;

    /**
     * User's full name.
     */
    private String fullName;

    /**
     * User's WhatsApp number.
     */
    private String whatsappNumber;

    /**
     * User's email address.
     */
    private String email;

    /**
     * User's 6-digit code number (for students).
     */
    private String codeNumber;
}

