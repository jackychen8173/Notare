CREATE TABLE courses (
    id UUID PRIMARY KEY,
    tutor_id UUID NOT NULL REFERENCES users(id),
    name VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    description TEXT
);

CREATE INDEX idx_courses_tutor_id ON courses(tutor_id);

CREATE TABLE enrollments (
    student_id UUID NOT NULL REFERENCES users(id),
    course_id UUID NOT NULL REFERENCES courses(id),
    enrolled_at TIMESTAMP NOT NULL DEFAULT now(),
    PRIMARY KEY (student_id, course_id)
);

CREATE INDEX idx_enrollments_course_id ON enrollments(course_id);
