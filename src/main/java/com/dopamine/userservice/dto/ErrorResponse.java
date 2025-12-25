package com.dopamine.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Error response DTO for API errors.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {

    /**
     * Error code identifying the type of error.
     */
    private String error;

    /**
     * Human-readable error message.
     */
    private String message;
}

