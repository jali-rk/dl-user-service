package com.dopamine.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for student registration.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentRegistrationResponse {
    private UserPublicView user;
    private boolean verificationCodeGenerated;
}

