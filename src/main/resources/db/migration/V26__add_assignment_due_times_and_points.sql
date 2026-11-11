-- Due times, points and late rules for assignments (roadmap stage 2, step 1).
--
-- Every due date becomes a moment in time, interpreted in its course's time zone. An assignment
-- can also open later (students don't see it until opens_at), stop taking submissions
-- (closes_at), take a late penalty per day, and be worth points.

-- 1. Courses get a time zone. Existing courses are all in BC, where Notare starts.
ALTER TABLE courses ADD COLUMN time_zone VARCHAR(64) NOT NULL DEFAULT 'America/Vancouver';

-- 2. Assignments get the new fields.
ALTER TABLE assignments
    ADD COLUMN due_at TIMESTAMPTZ,
    ADD COLUMN opens_at TIMESTAMPTZ,
    ADD COLUMN closes_at TIMESTAMPTZ,
    ADD COLUMN points_possible INTEGER,
    ADD COLUMN late_penalty_percent INTEGER NOT NULL DEFAULT 0;

-- due_at is filled from due_date by V27, a Java migration: the time zone rules have to come
-- from the JVM, the same rules the app uses, not from Postgres's copy. They can differ, and did
-- when BC moved to permanent UTC-7 on 2026-11-01 (tzdata 2026b).

-- due_date is no longer read or written. It stays (now nullable) so this migration loses nothing;
-- a later migration drops it once due_at has been in production for a while.
ALTER TABLE assignments ALTER COLUMN due_date DROP NOT NULL;

ALTER TABLE assignments
    ADD CONSTRAINT assignments_points_possible_positive
        CHECK (points_possible IS NULL OR points_possible > 0),
    ADD CONSTRAINT assignments_late_penalty_range
        CHECK (late_penalty_percent BETWEEN 0 AND 100),
    ADD CONSTRAINT assignments_opens_before_due
        CHECK (opens_at IS NULL OR opens_at < due_at),
    -- closes_at = due_at means "lock at the due time".
    ADD CONSTRAINT assignments_closes_after_due
        CHECK (closes_at IS NULL OR closes_at >= due_at);

-- 3. Submissions get points earned. The free-text grade column stays as it is.
ALTER TABLE submissions
    ADD COLUMN points_earned NUMERIC(7, 2)
        CONSTRAINT submissions_points_earned_not_negative CHECK (points_earned IS NULL OR points_earned >= 0);

-- 4. Convert existing grades that are clearly points: "8/10", "8.5 / 10", "85%".
--    An assignment gets points possible only when every such grade on it agrees on the total
--    (a percentage counts as out of 100). Letter grades, bare numbers ("85" could mean 85/100 or
--    8.5/10) and mixed totals are left as text and still show as the old grade.
CREATE TEMP TABLE parsed_grades ON COMMIT DROP AS
SELECT s.id AS submission_id,
       s.assignment_id,
       COALESCE(f[1], p[1])::NUMERIC(7, 2) AS earned,
       CASE WHEN f IS NOT NULL THEN f[2]::INTEGER ELSE 100 END AS possible
FROM submissions s
CROSS JOIN LATERAL (SELECT regexp_match(s.grade, '^\s*(\d{1,4}(?:\.\d{1,2})?)\s*/\s*(\d{1,4})\s*$') AS f) frac
CROSS JOIN LATERAL (SELECT regexp_match(s.grade, '^\s*(\d{1,3}(?:\.\d{1,2})?)\s*%\s*$') AS p) pct
WHERE s.grade IS NOT NULL
  AND (f IS NOT NULL OR p IS NOT NULL);

UPDATE assignments a
SET points_possible = agreed.possible
FROM (
    SELECT assignment_id, MIN(possible) AS possible
    FROM parsed_grades
    GROUP BY assignment_id
    HAVING COUNT(DISTINCT possible) = 1 AND MIN(possible) > 0
) agreed
WHERE a.id = agreed.assignment_id;

UPDATE submissions s
SET points_earned = pg.earned
FROM parsed_grades pg
JOIN assignments a ON a.id = pg.assignment_id
WHERE s.id = pg.submission_id
  AND a.points_possible = pg.possible;
