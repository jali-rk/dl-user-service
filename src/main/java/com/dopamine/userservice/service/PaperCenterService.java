package com.dopamine.userservice.service;
import com.dopamine.userservice.dto.CreatePaperCenterRequest;
import com.dopamine.userservice.dto.PaperCenterListResponse;
import com.dopamine.userservice.dto.PaperCenterResponse;
import java.util.UUID;
/**
 * Service interface for paper center management.
 */
public interface PaperCenterService {
    /**
     * Get all paper centers.
     */
    PaperCenterListResponse getAllPaperCenters();
    /**
     * Create a new paper center.
     */
    PaperCenterResponse createPaperCenter(CreatePaperCenterRequest request);
    /**
     * Delete a paper center by ID.
     */
    void deletePaperCenter(UUID centerId);
}
