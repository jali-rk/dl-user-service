package com.dopamine.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for confirming an email reset using the verification token.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailResetConfirmRequest {

    @NotBlank(message = "Token is required")
    private String token;
}

