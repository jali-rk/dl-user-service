package com.dopamine.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for sending email via BFF notification service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailNotificationRequest {

    /**
     * Recipient email address.
     */
    private String to;

    /**
     * Email subject.
     */
    private String subject;

    /**
     * Email body (can be plain text or HTML).
     */
    private String body;

    /**
     * Optional: template name if BFF supports templating.
     */
    private String templateName;

    /**
     * Optional: template variables for dynamic content.
     */
    private java.util.Map<String, Object> templateVariables;
}

