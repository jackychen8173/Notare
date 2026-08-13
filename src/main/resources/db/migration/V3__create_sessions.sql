CREATE TABLE sessions (
    id UUID PRIMARY KEY,
    tutor_id UUID NOT NULL REFERENCES users(id),
    student_id UUID NOT NULL REFERENCES users(id),
    course_id UUID REFERENCES courses(id),
    date TIMESTAMP NOT NULL,
    subject VARCHAR(255) NOT NULL,
    duration INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('SCHEDULED', 'COMPLETED', 'CANCELLED'))
);

CREATE INDEX idx_sessions_tutor_id ON sessions(tutor_id);
CREATE INDEX idx_sessions_student_id ON sessions(student_id);

CREATE TABLE session_notes (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL UNIQUE REFERENCES sessions(id),
    raw_notes TEXT,
    formatted_notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
