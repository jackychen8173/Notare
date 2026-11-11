-- Calendar subscribe links and quiz due dates (roadmap stage 2, step 3).
--
-- calendar_token is the secret part of a person's calendar feed URL
-- (/api/calendar/<token>.ics on the frontend). NULL until they ask for a link; resetting the link
-- replaces it, so an old URL stops working. Postgres allows many NULLs under a unique index.
ALTER TABLE users ADD COLUMN calendar_token VARCHAR(64);
CREATE UNIQUE INDEX users_calendar_token_key ON users (calendar_token);

-- Optional, informational: a quiz past its due time still accepts its one attempt, and the
-- gradebook flags it as missing when nobody started one.
ALTER TABLE quizzes ADD COLUMN due_at TIMESTAMPTZ;
