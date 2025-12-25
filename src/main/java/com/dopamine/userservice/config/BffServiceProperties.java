package com.dopamine.userservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for BFF service integration.
 */
@Configuration
@ConfigurationProperties(prefix = "bff.service")
@Getter
@Setter
public class BffServiceProperties {

    /**
     * Base URL of the BFF service.
     */
    private String baseUrl = "http://localhost:8081";

    /**
     * Internal service token for authentication.
     */
    private String internalToken;

    /**
     * Connection timeout in seconds.
     */
    private int connectTimeout = 5;

    /**
     * Read timeout in seconds.
     */
    private int readTimeout = 10;
}

