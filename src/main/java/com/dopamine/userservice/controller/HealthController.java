package com.dopamine.userservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Health check endpoint for the User Service.
 * This is a simple, custom health endpoint suitable for ALB/load balancer health checks.
 *
 * For detailed health information with full Spring Boot Actuator features, use:
 * - /actuator/health (with context path: /userservice/actuator/health)
 * - /actuator/info
 * - /actuator/metrics
 *
 * This endpoint does not require authentication and always returns a simple UP status.
 * Use Actuator endpoints for detailed monitoring and metrics.
 */
@RestController
public class HealthController {

    /**
     * Simple health check for load balancers.
     * Returns a minimal response optimized for ALB health checks.
     *
     * With context path, accessible at: /userservice/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "userservice"
        ));
    }
}

