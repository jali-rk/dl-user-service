package com.dopamine.userservice.config;

import com.dopamine.userservice.security.ServiceAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Security configuration for the User Service.
 * - Configures BCrypt password encoder
 * - Sets up service-to-service authentication filter
 * - Disables session management (stateless service)
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final ServiceAuthFilter serviceAuthFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(ServiceAuthFilter serviceAuthFilter, CorsConfigurationSource corsConfigurationSource) {
        this.serviceAuthFilter = serviceAuthFilter;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    /**
     * BCrypt password encoder bean.
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Security filter chain configuration.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF as this is a stateless service
                .csrf(AbstractHttpConfigurer::disable)

                // Enable CORS with configured settings from CorsConfig
                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                // Stateless session management
                .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Authorization rules - order matters!
                .authorizeHttpRequests(auth -> auth
                    // Explicitly allow health and actuator endpoints without ANY authentication
                    // Include both relative paths (after context stripping) and full paths (to be safe)
                    .requestMatchers(
                        "/health",
                        "/actuator",
                        "/actuator/**",
                        "/userservice/health",
                        "/userservice/actuator",
                        "/userservice/actuator/**"
                    ).permitAll()
                    // All other requests require service token authentication (handled by ServiceAuthFilter)
                    .anyRequest().authenticated()
                )

                // Add service authentication filter before username/password auth filter
                .addFilterBefore(serviceAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

