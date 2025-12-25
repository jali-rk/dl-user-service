package com.dopamine.userservice.service.impl;

import com.dopamine.userservice.config.BffServiceProperties;
import com.dopamine.userservice.constants.ApplicationConstants;
import com.dopamine.userservice.constants.EmailType;
import com.dopamine.userservice.dto.EmailNotificationRequest;
import com.dopamine.userservice.service.EmailNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Implementation of EmailNotificationService.
 * Sends emails by calling BFF's broadcast notification endpoint.
 */
@Service
@Slf4j
public class EmailNotificationServiceImpl implements EmailNotificationService {

    private final RestTemplate restTemplate;
    private final BffServiceProperties bffProperties;

    public EmailNotificationServiceImpl(
            RestTemplateBuilder restTemplateBuilder,
            BffServiceProperties bffProperties
    ) {
        this.bffProperties = bffProperties;
        this.restTemplate = restTemplateBuilder
                .rootUri(bffProperties.getBaseUrl())
                .build();
    }

    @Override
    public void sendVerificationCodeEmail(String email, String code) {
        log.info("Sending verification code email to: {}", email);

        // Note: Using email as targetUserId since user might not have UUID yet during registration
        // The BFF broadcast service should handle email-based targeting
        EmailNotificationRequest request = EmailNotificationRequest.builder()
                .targetUserIds(java.util.List.of(email))
                .channels(java.util.List.of("EMAIL"))
                .title(ApplicationConstants.Email.SUBJECT_VERIFICATION_CODE)
                .body(ApplicationConstants.Email.BODY_VERIFICATION_CODE.formatted(code))
                .build();

        sendEmail(request, EmailType.STUDENT_REGISTERED);
    }

    @Override
    public void sendResendVerificationCodeEmail(String email, String code) {
        log.info("Sending resend verification code email to: {}", email);

        EmailNotificationRequest request = EmailNotificationRequest.builder()
                .targetUserIds(java.util.List.of(email))
                .channels(java.util.List.of("EMAIL"))
                .title(ApplicationConstants.Email.SUBJECT_RESEND_VERIFICATION_CODE)
                .body(ApplicationConstants.Email.BODY_RESEND_VERIFICATION_CODE.formatted(code))
                .build();

        sendEmail(request, EmailType.STUDENT_REGISTERED);
    }

    /**
     * Send email via BFF broadcast notification endpoint.
     */
    private void sendEmail(EmailNotificationRequest request, EmailType emailType) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Service-Token", "change-me-in-production");

            HttpEntity<EmailNotificationRequest> httpEntity = new HttpEntity<>(request, headers);

            restTemplate.postForEntity(
                    ApplicationConstants.Email.BFF_NOTIFICATION_ENDPOINT,
                    httpEntity,
                    Void.class
            );

            log.info("Successfully sent {} email via broadcast to: {}", emailType, request.getTargetUserIds());

        } catch (RestClientException e) {
            log.error("Failed to send {} email to: {}. Error: {}", emailType, request.getTargetUserIds(), e.getMessage(), e);
            // Don't throw exception - email sending failure should not break the main flow
            // The calling service should continue (e.g., user registration should still succeed)
        }
    }
}

