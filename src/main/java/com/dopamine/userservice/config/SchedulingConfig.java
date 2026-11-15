package com.dopamine.userservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables @Scheduled jobs (e.g. login history retention).
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
