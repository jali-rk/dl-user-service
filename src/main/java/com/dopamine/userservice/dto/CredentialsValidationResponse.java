package com.dopamine.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for credentials validation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CredentialsValidationResponse {
    private boolean valid;
    private UserPublicView user;
}

