package com.dopamine.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic error object for API responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorObject {
    private String code;
    private String message;
    private Object details;
}

