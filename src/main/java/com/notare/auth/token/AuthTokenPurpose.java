package com.notare.auth.token;

import java.time.Duration;

public enum AuthTokenPurpose {
    PASSWORD_RESET(Duration.ofMinutes(30)),
    EMAIL_VERIFICATION(Duration.ofDays(3));

    private final Duration lifetime;

    AuthTokenPurpose(Duration lifetime) {
        this.lifetime = lifetime;
    }

    public Duration lifetime() {
        return lifetime;
    }
}
