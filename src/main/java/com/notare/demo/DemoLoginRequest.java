package com.notare.demo;

import com.notare.user.UserRole;
import jakarta.validation.constraints.NotNull;

/** Which side of the demo to sign in as: TUTOR or STUDENT. */
public record DemoLoginRequest(
        @NotNull UserRole role
) {
}
