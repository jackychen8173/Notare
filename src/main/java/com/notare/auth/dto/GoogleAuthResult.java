package com.notare.auth.dto;

// A single combined sign-in-or-sign-up response: existing account -> session is populated and
// needsRole is false. Brand-new Google account with no role supplied yet -> needsRole is true and
// session is null; the frontend then re-POSTs the same idToken with a role chosen, which lands
// back here with an existing-user match this time.
public record GoogleAuthResult(
        boolean needsRole,
        AuthResponse session
) {
}
