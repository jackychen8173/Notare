package com.notare.student.dto;

import com.notare.user.User;

import java.time.LocalDateTime;
import java.util.UUID;

public record StudentResponse(
        UUID id,
        String name,
        String email,
        LocalDateTime createdAt
) {
    public static StudentResponse from(User user) {
        return new StudentResponse(user.getId(), user.getName(), user.getEmail(), user.getCreatedAt());
    }
}
