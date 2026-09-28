-- "Report a problem" submissions from signed-in users, triaged in the admin portal.
CREATE TABLE problem_reports (
    id UUID PRIMARY KEY,
    reporter_id UUID NOT NULL REFERENCES users(id),
    category VARCHAR(20) NOT NULL CHECK (category IN ('BUG', 'CONFUSING', 'SUGGESTION', 'OTHER')),
    message TEXT NOT NULL,
    page_url VARCHAR(2048),
    user_agent VARCHAR(512),
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'RESOLVED')),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    resolved_at TIMESTAMP
);

CREATE INDEX idx_problem_reports_status_created_at ON problem_reports(status, created_at DESC);
