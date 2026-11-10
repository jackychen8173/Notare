-- docs/admin-seed-example.sql
--
-- Manual, one-time step for creating the first ADMIN account. Run it yourself against the
-- target database (local or production). Claude never runs it, per the "no direct DB mutations"
-- project rule.
--
-- 1. Sign up normally on the site with the email the admin will use, as a TUTOR, and create
--    nothing with it. Use a fresh account: an admin has no tutor pages, so any courses or
--    students it owned would be stranded. Sign-up handles the password hash, the email
--    verification flag and every other column, so nothing here needs a hand-made bcrypt hash.
-- 2. Replace the email below and run this. It changes exactly one row, and only if that account
--    owns no courses.
-- 3. Sign out and back in: the role is read at sign-in, so the old session stays a tutor's.

UPDATE users
SET role = 'ADMIN'
WHERE email = 'admin@example.com'
  AND role = 'TUTOR'
  AND demo = false
  AND NOT EXISTS (SELECT 1 FROM courses WHERE courses.tutor_id = users.id);

-- Expect "UPDATE 1". "UPDATE 0" means the email is wrong, or the account isn't a plain tutor
-- account with no courses.
