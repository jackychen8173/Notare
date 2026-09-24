package com.notare.auth.dto;

import com.notare.user.UserRole;
import jakarta.validation.constraints.NotBlank;

public record GoogleAuthRequest(
        @NotBlank String idToken,
        // Only read on the register path - ignored (and may be omitted) on login, since an
        // existing account's role was already decided when it was first created.
        UserRole role
) {
}
