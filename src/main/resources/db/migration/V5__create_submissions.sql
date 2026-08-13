CREATE TABLE submissions (
    id UUID PRIMARY KEY,
    assignment_id UUID NOT NULL REFERENCES assignments(id),
    student_id UUID NOT NULL REFERENCES users(id),
    content TEXT NOT NULL,
    sage_feedback JSONB,
    tutor_feedback TEXT,
    feedback_status VARCHAR(20) NOT NULL CHECK (feedback_status IN ('PENDING', 'APPROVED', 'REVISED')),
    grade VARCHAR(10),
    submitted_at TIMESTAMP NOT NULL DEFAULT now(),
    released_at TIMESTAMP
);

CREATE INDEX idx_submissions_assignment_id ON submissions(assignment_id);
CREATE INDEX idx_submissions_student_id ON submissions(student_id);
