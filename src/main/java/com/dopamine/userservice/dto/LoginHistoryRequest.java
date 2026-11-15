package com.dopamine.userservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for recording a successful login.
 * Called internally by the BFF.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginHistoryRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @Size(max = 255, message = "IP address must be at most 255 characters")
    private String ipAddress;

    private String userAgent;
}
