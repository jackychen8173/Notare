-- Course discussions: one thread model with a visibility flag, not a separate forum + messaging system.
-- PUBLIC threads are readable by the course's tutor and every enrolled student; PRIVATE threads only by
-- the student who started them and the course's tutor (Piazza/Ed-style "private to instructors").
-- The thread row holds the opening post (title + body); discussion_posts are replies only.
--
-- anonymous hides the author's name from *other students* only - the tutor always sees the real name.
-- It's meaningless on a PRIVATE thread (only the tutor can see it anyway) and is forced false there,
-- until the tutor makes the thread public, at which point the student author's thread + replies are
-- flipped to anonymous so a name shared only with the tutor is never exposed to classmates.
CREATE TABLE discussion_threads (
    id UUID PRIMARY KEY,
    course_id UUID NOT NULL REFERENCES courses(id),
    author_id UUID NOT NULL REFERENCES users(id),
    visibility VARCHAR(20) NOT NULL CHECK (visibility IN ('PUBLIC', 'PRIVATE')),
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    anonymous BOOLEAN NOT NULL DEFAULT false,
    pinned BOOLEAN NOT NULL DEFAULT false,
    locked BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    edited_at TIMESTAMP,
    last_activity_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_discussion_threads_course_id ON discussion_threads(course_id);

CREATE TABLE discussion_posts (
    id UUID PRIMARY KEY,
    thread_id UUID NOT NULL REFERENCES discussion_threads(id) ON DELETE CASCADE,
    author_id UUID NOT NULL REFERENCES users(id),
    body TEXT NOT NULL,
    anonymous BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    edited_at TIMESTAMP
);

CREATE INDEX idx_discussion_posts_thread_id ON discussion_posts(thread_id);

-- Per-user read marker for unread badges: a thread is unread when last_activity_at > last_read_at
-- (or no row exists yet).
CREATE TABLE discussion_thread_reads (
    id UUID PRIMARY KEY,
    thread_id UUID NOT NULL REFERENCES discussion_threads(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id),
    last_read_at TIMESTAMP NOT NULL,
    UNIQUE (thread_id, user_id)
);
