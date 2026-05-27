package com.dopamine.userservice.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
/**
 * Response DTO for paper center list.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaperCenterListResponse {
    private List<PaperCenterResponse> items;
    private int total;
    private long activeCount;
    private long deletedCount;
}
