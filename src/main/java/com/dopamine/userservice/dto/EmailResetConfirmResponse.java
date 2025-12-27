package com.dopamine.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for successful email reset confirmation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailResetConfirmResponse {
    private String newEmail;
}

