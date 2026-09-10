CREATE TABLE materials (
    id UUID PRIMARY KEY,
    course_id UUID NOT NULL REFERENCES courses(id),
    topic_id UUID REFERENCES topics(id) ON DELETE SET NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    url VARCHAR(2048),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_materials_course_id ON materials(course_id);
