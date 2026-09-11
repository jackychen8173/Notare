CREATE TABLE rubrics (
    id UUID PRIMARY KEY,
    assignment_id UUID NOT NULL UNIQUE REFERENCES assignments(id),
    title VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE rubric_criteria (
    id UUID PRIMARY KEY,
    rubric_id UUID NOT NULL REFERENCES rubrics(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    points_possible NUMERIC(6,2) NOT NULL,
    position INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_rubric_criteria_rubric_id ON rubric_criteria(rubric_id);

-- Additive to submissions - doesn't touch submissions.grade, which stays free text.
CREATE TABLE submission_criterion_scores (
    id UUID PRIMARY KEY,
    submission_id UUID NOT NULL REFERENCES submissions(id),
    criterion_id UUID NOT NULL REFERENCES rubric_criteria(id) ON DELETE CASCADE,
    points_awarded NUMERIC(6,2) NOT NULL,
    UNIQUE(submission_id, criterion_id)
);

CREATE INDEX idx_submission_criterion_scores_submission_id ON submission_criterion_scores(submission_id);
