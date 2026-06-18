package com.dopamine.userservice.service.impl;
import com.dopamine.userservice.domain.PaperCenter;
import com.dopamine.userservice.dto.CreatePaperCenterRequest;
import com.dopamine.userservice.dto.PaperCenterListResponse;
import com.dopamine.userservice.dto.PaperCenterResponse;
import com.dopamine.userservice.exception.PaperCenterAlreadyExistsException;
import com.dopamine.userservice.exception.PaperCenterNotFoundException;
import com.dopamine.userservice.repository.PaperCenterRepository;
import com.dopamine.userservice.service.PaperCenterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
/**
 * Implementation of PaperCenterService.
 */
@Service
@Slf4j
public class PaperCenterServiceImpl implements PaperCenterService {
    private final PaperCenterRepository paperCenterRepository;
    public PaperCenterServiceImpl(PaperCenterRepository paperCenterRepository) {
        this.paperCenterRepository = paperCenterRepository;
    }
    @Override
    @Transactional(readOnly = true)
    public PaperCenterListResponse getAllPaperCenters(boolean includeDeleted) {
        log.debug("Fetching paper centers, includeDeleted: {}", includeDeleted);
        
        List<PaperCenter> centers;
        if (includeDeleted) {
            centers = paperCenterRepository.findAllIncludingDeleted();
        } else {
            centers = paperCenterRepository.findAllByOrderByNameAsc();
        }
        
        List<PaperCenterResponse> responses = centers.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        
        long activeCount = centers.stream().filter(pc -> !pc.isDeleted()).count();
        long deletedCount = centers.size() - activeCount;
        
        log.info("Found {} paper centers ({} active, {} deleted)", centers.size(), activeCount, deletedCount);
        
        return PaperCenterListResponse.builder()
                .items(responses)
                .total(centers.size())
                .activeCount(activeCount)
                .deletedCount(deletedCount)
                .build();
    }
    @Override
    @Transactional
    public PaperCenterResponse createPaperCenter(CreatePaperCenterRequest request) {
        log.info("Creating paper center with name: {}", request.getName());
        // Check if paper center already exists
        if (paperCenterRepository.existsActiveByName(request.getName())) {
            log.warn("Paper center with name {} already exists", request.getName());
            throw new PaperCenterAlreadyExistsException("Paper center with name '" + request.getName() + "' already exists");
        }
        PaperCenter paperCenter = PaperCenter.builder()
                .name(request.getName())
                .build();
        PaperCenter saved = paperCenterRepository.save(paperCenter);
        log.info("Created paper center with ID: {}", saved.getId());
        return mapToResponse(saved);
    }
    @Override
    @Transactional
    public void softDeletePaperCenter(UUID centerId) {
        log.info("Soft deleting paper center with ID: {}", centerId);
        
        int updatedRows = paperCenterRepository.softDeleteById(centerId);
        if (updatedRows == 0) {
            throw new PaperCenterNotFoundException("Paper center not found with ID: " + centerId);
        }
        
        log.info("Soft deleted paper center with ID: {}", centerId);
    }

    @Override
    @Transactional
    public PaperCenterResponse restorePaperCenter(UUID centerId) {
        log.info("Restoring paper center with ID: {}", centerId);
        
        // First, restore using native query
        int updatedRows = paperCenterRepository.restoreById(centerId);
        if (updatedRows == 0) {
            throw new PaperCenterNotFoundException("Paper center not found or already active with ID: " + centerId);
        }
        
        // Refresh the entity from database using native query (ignores @Where)
        PaperCenter restored = paperCenterRepository.findByIdIncludingDeleted(centerId)
            .orElseThrow(() -> new PaperCenterNotFoundException("Paper center not found after restore: " + centerId));
        
        log.info("Restored paper center: {}", restored.getName());
        return mapToResponse(restored);
    }

    private PaperCenterResponse mapToResponse(PaperCenter paperCenter) {
        return PaperCenterResponse.builder()
                .id(paperCenter.getId())
                .name(paperCenter.getName())
                .createdAt(paperCenter.getCreatedAt())
                .updatedAt(paperCenter.getUpdatedAt())
                .deletedAt(paperCenter.getDeletedAt())
                .build();
    }
}
