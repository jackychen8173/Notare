-- Quiz content: a quiz belongs to a course, questions belong to a quiz, options belong to a question.
-- published_at is a nullable-timestamp draft gate, same idiom as courses.archived_at / submissions.released_at:
-- a quiz is built incrementally (question by question) over several requests, so without a gate a
-- half-built quiz with zero questions would be visible to enrolled students the instant it's created.
CREATE TABLE quizzes (
    id UUID PRIMARY KEY,
    course_id UUID NOT NULL REFERENCES courses(id),
    topic_id UUID REFERENCES topics(id),
    grade_category_id UUID REFERENCES grade_categories(id),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    time_limit_minutes INT,
    published_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_quizzes_course_id ON quizzes(course_id);

-- reference_answer is optional: a tutor's own grading guide for SHORT_ANSWER/ESSAY questions, also
-- fed to Sage as context when drafting a suggested score/feedback for a student's free-text answer.
CREATE TABLE quiz_questions (
    id UUID PRIMARY KEY,
    quiz_id UUID NOT NULL REFERENCES quizzes(id) ON DELETE CASCADE,
    type VARCHAR(20) NOT NULL CHECK (type IN ('MULTIPLE_CHOICE', 'TRUE_FALSE', 'SHORT_ANSWER', 'ESSAY')),
    prompt TEXT NOT NULL,
    points_possible NUMERIC(6,2) NOT NULL,
    reference_answer TEXT,
    position INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_quiz_questions_quiz_id ON quiz_questions(quiz_id);

-- TRUE_FALSE questions get exactly two synthesized rows here ("True"/"False") rather than a separate
-- boolean column on quiz_questions, so MULTIPLE_CHOICE and TRUE_FALSE share one grading code path:
-- an answer's single selected_option_id -> option.is_correct. Single-correct only in v1 (radio, not
-- checkbox) - a multi-select join table is the natural extension later if ever needed.
CREATE TABLE quiz_question_options (
    id UUID PRIMARY KEY,
    question_id UUID NOT NULL REFERENCES quiz_questions(id) ON DELETE CASCADE,
    text VARCHAR(500) NOT NULL,
    is_correct BOOLEAN NOT NULL DEFAULT false,
    position INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_quiz_question_options_question_id ON quiz_question_options(question_id);

-- One attempt per (quiz, student), enforced here rather than only in application code - single-attempt
-- is a hard product rule, not a UI nicety. deadline_at is computed at start time from
-- quizzes.time_limit_minutes (null if the quiz is untimed). No score column: total earned/possible is
-- computed on the fly from quiz_answers, same as submissions never store a rubric total.
CREATE TABLE quiz_attempts (
    id UUID PRIMARY KEY,
    quiz_id UUID NOT NULL REFERENCES quizzes(id),
    student_id UUID NOT NULL REFERENCES users(id),
    status VARCHAR(20) NOT NULL CHECK (status IN ('IN_PROGRESS', 'SUBMITTED')),
    started_at TIMESTAMP NOT NULL DEFAULT now(),
    deadline_at TIMESTAMP,
    submitted_at TIMESTAMP,
    auto_submitted BOOLEAN NOT NULL DEFAULT false,
    released_at TIMESTAMP,
    UNIQUE(quiz_id, student_id)
);

CREATE INDEX idx_quiz_attempts_quiz_id ON quiz_attempts(quiz_id);
CREATE INDEX idx_quiz_attempts_student_id ON quiz_attempts(student_id);

-- selected_option_id (MULTIPLE_CHOICE/TRUE_FALSE) and text_response (SHORT_ANSWER/ESSAY) are mutually
-- exclusive depending on the question's type. is_correct/points_awarded are set automatically at
-- finalize time for MC/TF; for SHORT_ANSWER/ESSAY they stay null until a tutor grades them.
-- sage_suggestion mirrors submissions.sage_feedback exactly - never shown to the student until the
-- owning attempt is released, same trust boundary.
-- question_id cascades (same as rubric_criteria -> submission_criterion_scores in V12) so deleting a
-- question doesn't require an app-level "block delete if answered" check. selected_option_id is
-- SET NULL rather than CASCADE: editing a question's options (delete-and-recreate) shouldn't destroy
-- an already-submitted answer row, just its now-stale option reference.
CREATE TABLE quiz_answers (
    id UUID PRIMARY KEY,
    attempt_id UUID NOT NULL REFERENCES quiz_attempts(id) ON DELETE CASCADE,
    question_id UUID NOT NULL REFERENCES quiz_questions(id) ON DELETE CASCADE,
    selected_option_id UUID REFERENCES quiz_question_options(id) ON DELETE SET NULL,
    text_response TEXT,
    is_correct BOOLEAN,
    points_awarded NUMERIC(6,2),
    tutor_feedback TEXT,
    sage_suggestion JSONB,
    UNIQUE(attempt_id, question_id)
);

CREATE INDEX idx_quiz_answers_attempt_id ON quiz_answers(attempt_id);
