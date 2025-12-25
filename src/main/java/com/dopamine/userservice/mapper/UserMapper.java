package com.dopamine.userservice.mapper;

import com.dopamine.userservice.domain.User;
import com.dopamine.userservice.dto.StudentListItem;
import com.dopamine.userservice.dto.UserPublicBatchView;
import com.dopamine.userservice.dto.UserPublicView;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting User entities to DTOs and vice versa.
 */
@Component
public class UserMapper {

    /**
     * Convert User entity to UserPublicView DTO.
     * Never exposes sensitive data like password hash.
     */
    public UserPublicView toPublicView(User user) {
        if (user == null) {
            return null;
        }

        return UserPublicView.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .whatsappNumber(user.getWhatsappNumber())
                .school(user.getSchool())
                .address(user.getAddress())
                .role(user.getRole())
                .status(user.getStatus())
                .codeNumber(user.getCodeNumber())
                .isVerified(user.isVerified())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }

    /**
     * Convert User entity to UserPublicBatchView DTO.
     * Contains only minimal fields required by batch endpoint.
     */
    public UserPublicBatchView toPublicBatchView(User user) {
        if (user == null) {
            return null;
        }

        return UserPublicBatchView.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .whatsappNumber(user.getWhatsappNumber())
                .email(user.getEmail())
                .codeNumber(user.getCodeNumber())
                .build();
    }

    /**
     * Convert User entity to StudentListItem DTO.
     * Contains fields required for paginated student listing.
     */
    public StudentListItem toStudentListItem(User user) {
        if (user == null) {
            return null;
        }

        // Extract registration number from code number (e.g., "560001" -> 560001)
        Integer registrationNumber = null;
        if (user.getCodeNumber() != null) {
            try {
                registrationNumber = Integer.parseInt(user.getCodeNumber());
            } catch (NumberFormatException e) {
                // If code number is not numeric, leave as null
            }
        }

        return StudentListItem.builder()
                .id(user.getId().toString())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .whatsappNumber(user.getWhatsappNumber())
                .codeNumber(user.getCodeNumber())
                .role(user.getRole().name())
                .isVerified(user.isVerified())
                .registrationNumber(registrationNumber)
                .createdAt(user.getCreatedAt())
                .build();
    }
}

