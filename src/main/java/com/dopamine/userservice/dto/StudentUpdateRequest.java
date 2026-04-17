package com.dopamine.userservice.dto;
import java.util.UUID;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating student profile.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentUpdateRequest {

    @Size(max = 255, message = "Full name must not exceed 255 characters")
    private String fullName;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "WhatsApp number must be valid (E.164 format)")
    @Size(max = 20, message = "WhatsApp number must not exceed 20 characters")
    private String whatsappNumber;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Secondary phone number must be valid (E.164 format)")
    @Size(max = 20, message = "Secondary phone number must not exceed 20 characters")
    private String secondaryPhoneNumber;

    /**
     * National Identity Card number (NIC).
     * Nullable but editable.
     */
    private String nic;

    @Size(max = 255, message = "School name must not exceed 255 characters")
    private String school;

    private String address;

    /**
     * Paper writing mode as a string. Persisted as enum in {@link com.dopamine.userservice.domain.User}.
     * Expected values: ONLINE, PHYSICAL, NOT_WRITING (case-insensitive).
     */
    private String paperWritingMode;

    /**
     * Paper center name (nullable).
     */
    private UUID paperCenter;

    /**
     * Study medium as a string. Persisted as enum in {@link com.dopamine.userservice.domain.User}.
     * Expected values: SINHALA, TAMIL, ENGLISH (case-insensitive).
     */
    private String studyMedium;
}
