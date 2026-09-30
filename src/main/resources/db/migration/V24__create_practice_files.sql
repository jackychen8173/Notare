-- A student's own practice code, not tied to any course or assignment (the /student/workspace page).
CREATE TABLE practice_files (
    id UUID PRIMARY KEY,
    student_id UUID NOT NULL REFERENCES users(id),
    name VARCHAR(100) NOT NULL,
    content TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_practice_files_student_id ON practice_files(student_id);
