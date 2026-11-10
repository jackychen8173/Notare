-- Password reset and email verification (stage 1 of the roadmap).

-- Existing accounts start unverified. Google sign-ins mark themselves verified on their next
-- login (Google already verified the address); password accounts verify through the emailed link.
ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;

-- Set when the password changes. JwtAuthenticationFilter rejects tokens issued before it, so a
-- password reset signs out every existing session.
ALTER TABLE users ADD COLUMN password_changed_at TIMESTAMP;

-- One-time tokens sent by email. Only a SHA-256 hash of the token is stored, so a database leak
-- doesn't hand out working reset links.
CREATE TABLE auth_tokens (
    id          UUID PRIMARY KEY,
    user_id     UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    purpose     VARCHAR(32) NOT NULL,
    token_hash  VARCHAR(64) NOT NULL UNIQUE,
    expires_at  TIMESTAMP   NOT NULL,
    used_at     TIMESTAMP,
    created_at  TIMESTAMP   NOT NULL
);

CREATE INDEX idx_auth_tokens_user_purpose ON auth_tokens (user_id, purpose);
