package com.notare.auth.dto;

import com.notare.user.UserRole;

import java.util.UUID;

public record AuthResponse(
        String token,
        UUID userId,
        String name,
        String email,
        UserRole role,
        // True for the landing page's one-click demo accounts (Sage and code Run are off for them).
        boolean demo
) {
}
