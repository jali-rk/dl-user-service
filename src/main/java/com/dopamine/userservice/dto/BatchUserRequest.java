package com.dopamine.userservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for batch user data retrieval.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchUserRequest {

    @NotNull(message = "userIds must not be null")
    @Size(min = 1, max = 1000, message = "userIds must contain between 1 and 1000 user IDs")
    private List<UUID> userIds;
}

