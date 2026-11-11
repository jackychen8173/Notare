-- Text files a teacher attaches to an assignment (a CSV, a word list) for students' programs to
-- read when they press Run. Small and text-only, so they live here rather than in the bucket.
CREATE TABLE assignment_data_files (
    id UUID PRIMARY KEY,
    assignment_id UUID NOT NULL REFERENCES assignments(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_assignment_data_files_name UNIQUE (assignment_id, name)
);

CREATE INDEX idx_assignment_data_files_assignment_id ON assignment_data_files(assignment_id);
