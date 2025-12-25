package com.dopamine.userservice.mapper;

import com.dopamine.userservice.domain.User;
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
}

