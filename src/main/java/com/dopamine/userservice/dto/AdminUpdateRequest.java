package com.dopamine.userservice.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating admin profile.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUpdateRequest {

    @Size(max = 255, message = "Full name must not exceed 255 characters")
    private String fullName;
}

