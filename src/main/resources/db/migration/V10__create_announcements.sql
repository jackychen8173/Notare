CREATE TABLE announcements (
    id UUID PRIMARY KEY,
    course_id UUID NOT NULL REFERENCES courses(id),
    tutor_id UUID NOT NULL REFERENCES users(id),
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_announcements_course_id ON announcements(course_id);
