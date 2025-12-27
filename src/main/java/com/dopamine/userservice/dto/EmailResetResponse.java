package com.dopamine.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for email reset request.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailResetResponse {
    private String token;
}

