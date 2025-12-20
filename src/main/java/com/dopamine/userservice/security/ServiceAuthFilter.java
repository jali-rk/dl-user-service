package com.dopamine.userservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * Filter to authenticate internal service-to-service calls.
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

        // Skip authentication for actuator endpoints and health checks
        // Note: With context path /userservice, paths will be /userservice/actuator/* and /userservice/health
        String requestPath = request.getRequestURI();

        // Allow health endpoints (both custom and actuator)
        if (requestPath.endsWith("/health") ||
            requestPath.contains("/actuator/") ||
            requestPath.endsWith("/actuator")) {
            log.debug("Allowing unauthenticated access to health/actuator endpoint: {}", requestPath);
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

        // Token is valid, set authentication in SecurityContext
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "internal-service",
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_SERVICE"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Proceed with request
        filterChain.doFilter(request, response);
    }
}
