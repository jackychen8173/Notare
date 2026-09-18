package com.notare.admin.dto;

import com.notare.user.User;
import com.notare.user.UserRole;

import java.time.LocalDateTime;
import java.util.UUID;

public record AdminUserResponse(
        UUID id,
        String name,
        String email,
        UserRole role,
        boolean active,
        LocalDateTime createdAt
) {
    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt()
        );
    }
}
