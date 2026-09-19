-- Nullable timestamp doubles as the archived flag and the "when": archived_at IS NULL means active.
-- Same pattern as submissions.released_at.
ALTER TABLE courses ADD COLUMN archived_at TIMESTAMP;
