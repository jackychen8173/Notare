-- Lets a tutor pick how a material should render: a plain link (today's only behavior, kept as the
-- default so every existing row is unaffected), an embedded Google Doc/Slides preview, or an
-- embedded PDF. Default 'LINK' backfills all existing materials to today's exact behavior.
ALTER TABLE materials
    ADD COLUMN type VARCHAR(20) NOT NULL DEFAULT 'LINK'
        CHECK (type IN ('LINK', 'GOOGLE_DOC', 'GOOGLE_SLIDES', 'PDF'));
