-- Per-assignment opt-in: once feedback is released, the student may submit a new version.
ALTER TABLE assignments ADD COLUMN allow_resubmission BOOLEAN NOT NULL DEFAULT FALSE;

-- Each submission row is one numbered version of a student's work on an assignment.
ALTER TABLE submissions ADD COLUMN attempt_number INT NOT NULL DEFAULT 1;

-- Before this migration nothing stopped a second submission row, so number any that exist by time.
UPDATE submissions s
SET attempt_number = numbered.n
FROM (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY assignment_id, student_id ORDER BY submitted_at, id) AS n
    FROM submissions
) numbered
WHERE s.id = numbered.id;

ALTER TABLE submissions
    ADD CONSTRAINT uq_submissions_attempt UNIQUE (assignment_id, student_id, attempt_number);

-- Comments pinned to one line of a submission's code.
-- SUGGESTED rows are Sage drafts only the tutor sees; PUBLISHED rows reach the student once the
-- submission's feedback is released.
CREATE TABLE submission_line_comments (
    id UUID PRIMARY KEY,
    submission_id UUID NOT NULL REFERENCES submissions(id) ON DELETE CASCADE,
    line_number INT NOT NULL CHECK (line_number >= 1),
    body TEXT NOT NULL,
    source VARCHAR(10) NOT NULL CHECK (source IN ('TUTOR', 'SAGE')),
    status VARCHAR(10) NOT NULL CHECK (status IN ('SUGGESTED', 'PUBLISHED')),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_submission_line_comments_submission_id ON submission_line_comments(submission_id);
