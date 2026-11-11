-- In-app notifications (roadmap stage 2, step 4). Notifications aren't stored: they're worked out
-- from existing rows each time someone opens them (com.notare.notification), so nothing has to run
-- on a timer and wake the backend.

-- When an assignment was posted, for "new assignment" notifications. Left NULL for assignments that
-- existed before this migration: when they were posted isn't known, and they never show as new.
ALTER TABLE assignments ADD COLUMN created_at TIMESTAMPTZ;
ALTER TABLE assignments ALTER COLUMN created_at SET DEFAULT now();

-- Everything newer than this is unread. NULL: never opened notifications, so everything is unread.
ALTER TABLE users ADD COLUMN notifications_seen_at TIMESTAMPTZ;
-- Comma-separated NotificationKind names the person turned off; empty means none.
ALTER TABLE users ADD COLUMN muted_notifications VARCHAR(255) NOT NULL DEFAULT '';
