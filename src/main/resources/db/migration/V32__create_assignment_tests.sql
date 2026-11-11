-- Teacher-written output checks for an assignment: run the student's program with this input and
-- compare what it prints with the expected output. Hidden tests show students only pass or fail.
CREATE TABLE assignment_tests (
    id UUID PRIMARY KEY,
    assignment_id UUID NOT NULL REFERENCES assignments(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    stdin TEXT NOT NULL DEFAULT '',
    expected_output TEXT NOT NULL,
    points INTEGER NOT NULL DEFAULT 1 CHECK (points >= 0 AND points <= 100),
    hidden BOOLEAN NOT NULL DEFAULT FALSE,
    position INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_assignment_tests_assignment_id ON assignment_tests(assignment_id);

-- The latest test run on a submission, which the tutor starts from the review page (JSON list of
-- results). Tutor-only: it suggests a score, and nothing reaches the student until release.
ALTER TABLE submissions ADD COLUMN test_results TEXT;
ALTER TABLE submissions ADD COLUMN tests_run_at TIMESTAMP;
