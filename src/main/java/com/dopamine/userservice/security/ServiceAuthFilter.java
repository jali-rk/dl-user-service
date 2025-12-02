package com.dopamine.userservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter to authenticate internal service-to-service calls.
 * Validates the X-Service-Token header against configured token.
 */
@Component
@Slf4j
public class ServiceAuthFilter extends OncePerRequestFilter {

    private static final String SERVICE_TOKEN_HEADER = "X-Service-Token";

    @Value("${user.service.internalToken:}")
    private String internalToken;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Skip authentication for actuator endpoints (if any) and health checks
        String requestPath = request.getRequestURI();
        if (requestPath.startsWith("/actuator") || requestPath.equals("/health")) {
            filterChain.doFilter(request, response);
            return;
        }

        String providedToken = request.getHeader(SERVICE_TOKEN_HEADER);

        // Check if internal token is configured
        if (internalToken == null || internalToken.isEmpty()) {
            log.warn("Internal service token is not configured. Rejecting request.");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"code\":\"UNAUTHORIZED\",\"message\":\"Service authentication required\"}");
            return;
        }

        // Validate token
        if (providedToken == null || !providedToken.equals(internalToken)) {
            log.warn("Invalid or missing service token for request: {} {}", request.getMethod(), requestPath);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"code\":\"UNAUTHORIZED\",\"message\":\"Invalid service token\"}");
            return;
        }

        // Token is valid, proceed with request
        filterChain.doFilter(request, response);
    }
}

