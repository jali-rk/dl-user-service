package com.dopamine.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for resending verification code.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResendVerificationCodeResponse {

    private boolean success;
    private String message;
    private String code; // The new verification code (same as student code number)
}

