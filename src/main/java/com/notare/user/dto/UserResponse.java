package com.notare.user.dto;

import com.notare.user.User;
import com.notare.user.UserRole;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String email,
        UserRole role,
        LocalDateTime createdAt,
        boolean emailVerified,
        boolean demo
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt(),
                user.isEmailVerified(),
                user.isDemo()
        );
    }
}
