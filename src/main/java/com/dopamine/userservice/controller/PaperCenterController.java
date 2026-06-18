package com.dopamine.userservice.controller;
import com.dopamine.userservice.dto.CreatePaperCenterRequest;
import com.dopamine.userservice.dto.PaperCenterListResponse;
import com.dopamine.userservice.dto.PaperCenterResponse;
import com.dopamine.userservice.service.PaperCenterService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
/**
 * Controller for paper center management.
 * GET endpoint is public (no auth), POST and DELETE require service token authentication.
 */
@RestController
@RequestMapping("/paper-centers")
@Slf4j
public class PaperCenterController {
    private final PaperCenterService paperCenterService;
    public PaperCenterController(PaperCenterService paperCenterService) {
        this.paperCenterService = paperCenterService;
    }
    /**
     * Get all paper centers including softly deleted centers.
     * GET /paper-centers
     * Public endpoint - no authentication required
     */
    @GetMapping
    public ResponseEntity<PaperCenterListResponse> getAllPaperCenters(
            @RequestParam(defaultValue = "false") boolean includeDeleted) {
        log.debug("Get paper centers request, includeDeleted: {}", includeDeleted);
        PaperCenterListResponse response = paperCenterService.getAllPaperCenters(includeDeleted);
        return ResponseEntity.ok(response);
    }
    /**
     * Create a new paper center (Admin only).
     * POST /paper-centers
     * Requires authentication via X-Service-Token
     */
    @PostMapping
    public ResponseEntity<PaperCenterResponse> createPaperCenter(
            @Valid @RequestBody CreatePaperCenterRequest request) {
        log.info("Create paper center request: {}", request.getName());
        PaperCenterResponse response = paperCenterService.createPaperCenter(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    /**
     * Soft Deletion of a paper center (Admin only).
     * DELETE /paper-centers/{centerId}
     * Requires authentication via X-Service-Token
     */
    @DeleteMapping("/{centerId}")
    public ResponseEntity<Void> deletePaperCenter(@PathVariable UUID centerId) {
        log.info("Soft delete paper center request for ID: {}", centerId);
        paperCenterService.softDeletePaperCenter(centerId);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/{centerId}/restore")
    public ResponseEntity<PaperCenterResponse> restorePaperCenter(@PathVariable UUID centerId) {
        log.info("Restore paper center request for ID: {}", centerId);
        PaperCenterResponse response = paperCenterService.restorePaperCenter(centerId);
        return ResponseEntity.ok(response);
    }
}
