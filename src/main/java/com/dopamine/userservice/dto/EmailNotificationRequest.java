package com.dopamine.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for sending email via BFF broadcast notification service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailNotificationRequest {

    /**
     * Target user UUIDs (as strings) - required by BFF broadcast API.
     */
    private List<String> targetUserIds;

    /**
     * Delivery channels (e.g., ["EMAIL"]).
     */
    private List<String> channels;

    /**
     * Notification title.
     */
    private String title;

    /**
     * Notification body.
     */
    private String body;
}
