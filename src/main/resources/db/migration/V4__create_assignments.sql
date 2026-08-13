CREATE TABLE assignments (
    id UUID PRIMARY KEY,
    course_id UUID NOT NULL REFERENCES courses(id),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    due_date DATE NOT NULL
);

CREATE INDEX idx_assignments_course_id ON assignments(course_id);
