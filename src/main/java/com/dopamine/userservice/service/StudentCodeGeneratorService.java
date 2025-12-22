package com.dopamine.userservice.service;

/**
 * Service for generating unique hierarchical student codes.
 *
 * Code Structure: XYZZZZ (6 digits)
 * - X: Main pillar digit (1-9) - randomly selected
 * - Y: Sub-pillar digit (0-9) - randomly selected
 * - ZZZZ: Sequential number (0001-9999) - tracked per sub-pillar
 *
 * Example: 560009
 * - Random digit 1: 5 gives Main pillar: 500000
 * - Random digit 2: 6 gives Sub-pillar: 560000
 * - Sequential: 0009 (last issued was 560008)
 *
 * Features:
 * - Prevents code reuse through sequential tracking
 * - Hard to guess due to random pillar selection
 * - 81 sub-pillars, 9999 codes each = 809,919 total possible codes
 */
public interface StudentCodeGeneratorService {

    /**
     * Generate the next unique student code using hierarchical random structure.
     *
     * Algorithm:
     * 1. Generate random main pillar digit (1-9)
     * 2. Generate random sub-pillar digit (0-9)
     * 3. Calculate sub-pillar base: (main * 100000) + (sub * 10000)
     * 4. Get next sequential number from tracker
     * 5. If sub-pillar is at limit, regenerate random digits
     *
     * @return A unique 6-digit student code
     * @throws IllegalStateException if unable to generate code after maximum retries
     */
    String generateStudentCode();
}

