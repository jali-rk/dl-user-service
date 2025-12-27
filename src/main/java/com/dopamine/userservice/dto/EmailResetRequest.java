package com.dopamine.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for initiating an email reset (verify new email) flow.
 * Called internally by the BFF.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailResetRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotBlank(message = "Old email is required")
    @Email(message = "Old email must be valid")
    private String oldEmail;

    @NotBlank(message = "New email is required")
    @Email(message = "New email must be valid")
    private String newEmail;
}

