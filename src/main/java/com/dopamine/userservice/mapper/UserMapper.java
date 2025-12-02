package com.dopamine.userservice.mapper;

import com.dopamine.userservice.domain.User;
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
}

