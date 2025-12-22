package com.dopamine.userservice.service.impl;

import com.dopamine.userservice.domain.CodePillarTracker;
import com.dopamine.userservice.repository.CodePillarTrackerRepository;
import com.dopamine.userservice.service.StudentCodeGeneratorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

/**
 * Implementation of StudentCodeGeneratorService.
 * Generates unique hierarchical student codes with random pillar selection and sequential numbering.
 */
@Service
@Slf4j
public class StudentCodeGeneratorServiceImpl implements StudentCodeGeneratorService {

    private static final int MAX_RETRIES = 100; // Maximum attempts to find an available sub-pillar
    private static final SecureRandom random = new SecureRandom();

    private final CodePillarTrackerRepository codePillarTrackerRepository;

    public StudentCodeGeneratorServiceImpl(CodePillarTrackerRepository codePillarTrackerRepository) {
        this.codePillarTrackerRepository = codePillarTrackerRepository;
    }

    @Override
    @Transactional
    public String generateStudentCode() {
        log.debug("Starting student code generation");

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                // Step 1: Generate random main pillar digit (1-9)
                int mainPillarDigit = random.nextInt(9) + 1;
                int mainPillar = mainPillarDigit * 100000;

                // Step 2: Generate random sub-pillar digit (0-9)
                int subPillarDigit = random.nextInt(10);

                // Step 3: Calculate sub-pillar base
                int subPillarBase = mainPillar + (subPillarDigit * 10000);

                log.debug("Attempt {}: Generated main pillar digit: {}, sub-pillar digit: {}, sub-pillar base: {}",
                         attempt + 1, mainPillarDigit, subPillarDigit, subPillarBase);

                // Step 4: Get or create tracker for this sub-pillar with pessimistic lock
                CodePillarTracker tracker = codePillarTrackerRepository
                        .findBySubPillarBaseWithLock(subPillarBase)
                        .orElse(null);

                if (tracker == null) {
                    // First time using this sub-pillar - initialize with base + 1
                    tracker = CodePillarTracker.builder()
                            .subPillarBase(subPillarBase)
                            .lastIssuedNumber(subPillarBase) // Will be incremented to base + 1
                            .build();
                    log.info("Initializing new sub-pillar tracker for base: {}", subPillarBase);
                }

                // Step 5: Check if sub-pillar is at limit
                if (tracker.isAtLimit()) {
                    log.debug("Sub-pillar {} is at limit, trying different random selection", subPillarBase);
                    continue; // Try different random digits
                }

                // Step 6: Get next code number and update tracker
                int nextCodeNumber = tracker.getNextCodeNumber();
                tracker.setLastIssuedNumber(nextCodeNumber);
                codePillarTrackerRepository.save(tracker);

                String studentCode = String.valueOf(nextCodeNumber);
                log.info("Successfully generated student code: {} (sub-pillar: {}, attempt: {})",
                        studentCode, subPillarBase, attempt + 1);

                return studentCode;

            } catch (Exception e) {
                log.warn("Error during code generation attempt {}: {}", attempt + 1, e.getMessage());
                // Continue to next attempt
            }
        }

        // Should rarely happen unless system is near capacity
        String errorMsg = "Failed to generate student code after " + MAX_RETRIES + " attempts. System may be at capacity.";
        log.error(errorMsg);
        throw new IllegalStateException(errorMsg);
    }
}

