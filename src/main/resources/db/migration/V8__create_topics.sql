CREATE TABLE topics (
    id UUID PRIMARY KEY,
    course_id UUID NOT NULL REFERENCES courses(id),
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_topics_course_id ON topics(course_id);

-- Deleting a topic shouldn't take its assignments with it - they just become ungrouped.
ALTER TABLE assignments ADD COLUMN topic_id UUID REFERENCES topics(id) ON DELETE SET NULL;
