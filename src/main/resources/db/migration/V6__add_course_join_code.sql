ALTER TABLE courses ADD COLUMN join_code VARCHAR(8);

-- Backfill any pre-existing courses with a placeholder code before enforcing NOT NULL/UNIQUE.
-- New courses get a nicer ambiguous-character-free code from CourseService; this is a one-time
-- migration-only fallback for rows that predate the join_code column.
UPDATE courses
SET join_code = UPPER(SUBSTRING(REPLACE(gen_random_uuid()::text, '-', '') FROM 1 FOR 6))
WHERE join_code IS NULL;

ALTER TABLE courses ALTER COLUMN join_code SET NOT NULL;
ALTER TABLE courses ADD CONSTRAINT uq_courses_join_code UNIQUE (join_code);
