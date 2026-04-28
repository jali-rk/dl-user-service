package com.dopamine.userservice.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;
/**
 * Response DTO for paper center.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaperCenterResponse {
    private UUID id;
    private String name;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
    
    public boolean isDeleted() {
        return deletedAt != null;
    }
}
