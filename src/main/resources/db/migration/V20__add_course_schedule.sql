-- An optional weekly meeting schedule per course (e.g. Mon/Wed/Fri 09:00-09:50), shown on the calendar.
-- meeting_days is a comma-separated list of java.time.DayOfWeek names ("MONDAY,WEDNESDAY,FRIDAY").
-- Times are wall-clock times with no zone, rendered as-is in the viewer's browser, the same way
-- sessions.date is. The term dates optionally bound the recurrence; without them it repeats indefinitely.
ALTER TABLE courses
    ADD COLUMN meeting_days       VARCHAR(80),
    ADD COLUMN meeting_start_time TIME,
    ADD COLUMN meeting_end_time   TIME,
    ADD COLUMN term_start_date    DATE,
    ADD COLUMN term_end_date      DATE,
    ADD CONSTRAINT courses_meeting_times_check
        CHECK ((meeting_days IS NULL AND meeting_start_time IS NULL AND meeting_end_time IS NULL)
            OR (meeting_days IS NOT NULL AND meeting_start_time IS NOT NULL AND meeting_end_time IS NOT NULL
                AND meeting_end_time > meeting_start_time)),
    ADD CONSTRAINT courses_term_dates_check
        CHECK (term_start_date IS NULL OR term_end_date IS NULL OR term_end_date >= term_start_date);
