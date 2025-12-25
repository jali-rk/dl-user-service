package com.dopamine.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for sending email via BFF notification broadcast service.
 * Matches the broadcast API structure.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailNotificationRequest {

    /**
     * List of target user IDs to send the notification to.
     */
    private List<String> targetUserIds;

    /**
     * List of channels to send through (e.g., ["EMAIL"]).
     */
    private List<String> channels;

    /**
     * Email title/subject.
     */
    private String title;

    /**
     * Email body content.
     */
    private String body;
}

