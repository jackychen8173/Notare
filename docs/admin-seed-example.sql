-- docs/admin-seed-example.sql
--
-- Manual, one-time example for creating the first ADMIN account. Run this
-- yourself against the target database (local or production) — it is never
-- executed by Claude, per the "no direct DB mutations" project rule.
--
-- 1. Generate a bcrypt hash for the admin's password, e.g. in a local Java/Node
--    REPL: new BCryptPasswordEncoder().encode("your-password-here")
-- 2. Replace the placeholders below and run the INSERT.

INSERT INTO users (id, name, email, password, role, active, created_at)
VALUES (
    gen_random_uuid(),
    'Admin Name',
    'admin@example.com',
    '$2a$10$REPLACE_WITH_A_REAL_BCRYPT_HASH',
    'ADMIN',
    true,
    now()
);
