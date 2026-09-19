CREATE TABLE grade_categories (
    id UUID PRIMARY KEY,
    course_id UUID NOT NULL REFERENCES courses(id),
    name VARCHAR(255) NOT NULL,
    weight_percent NUMERIC(5,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_grade_categories_course_id ON grade_categories(course_id);

-- Organizational only for now - no aggregate/weighted grade computation reads this column yet.
ALTER TABLE assignments ADD COLUMN grade_category_id UUID REFERENCES grade_categories(id) ON DELETE SET NULL;
