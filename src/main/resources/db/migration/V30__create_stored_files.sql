-- File uploads (roadmap stage 2, step 6): course materials a tutor uploads, and files a student
-- attaches to a submission. The bytes live in object storage (a Railway bucket); this table holds
-- who may see each one.
--
-- Exactly one of material_id / assignment_id is set:
--   material_id    a course material the tutor uploaded (materials.type = 'FILE').
--   assignment_id  a student's file for that assignment. submission_id is NULL while it's a draft
--                  for their next submission, and set when they submit.
-- Rows removed by a cascade (a deleted course, say) leave their object in the bucket; the app
-- deletes objects itself when it deletes a file or a material.
CREATE TABLE stored_files (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    uploader_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    material_id UUID REFERENCES materials(id) ON DELETE CASCADE,
    assignment_id UUID REFERENCES assignments(id) ON DELETE CASCADE,
    submission_id UUID REFERENCES submissions(id) ON DELETE CASCADE,
    object_key VARCHAR(255) NOT NULL UNIQUE,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(127) NOT NULL,
    size_bytes BIGINT NOT NULL CHECK (size_bytes >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK ((material_id IS NULL) <> (assignment_id IS NULL)),
    CHECK (submission_id IS NULL OR assignment_id IS NOT NULL)
);

CREATE INDEX stored_files_uploader_idx ON stored_files (uploader_id);
CREATE INDEX stored_files_material_idx ON stored_files (material_id);
CREATE INDEX stored_files_submission_idx ON stored_files (submission_id);
CREATE INDEX stored_files_drafts_idx ON stored_files (assignment_id, uploader_id) WHERE submission_id IS NULL;

-- A material can now be an uploaded file.
ALTER TABLE materials DROP CONSTRAINT IF EXISTS materials_type_check;
ALTER TABLE materials ADD CONSTRAINT materials_type_check
    CHECK (type IN ('LINK', 'GOOGLE_DOC', 'GOOGLE_SLIDES', 'PDF', 'FILE'));
