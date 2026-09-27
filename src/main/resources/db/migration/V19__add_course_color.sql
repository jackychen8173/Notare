-- A per-course accent color, shown as the course banner, the sidebar dot and on course cards, so a
-- tutor running several sections can tell them apart at a glance. Stored as a palette key rather than
-- a hex value so the frontend can render each key with separate light- and dark-theme shades.
-- GREEN is deliberately absent: green is reserved for Sage (AI) content in the design system.
ALTER TABLE courses
    ADD COLUMN color VARCHAR(20) NOT NULL DEFAULT 'TEAL'
        CHECK (color IN ('TEAL', 'BLUE', 'INDIGO', 'VIOLET', 'ROSE', 'ORANGE', 'AMBER', 'SLATE'));

-- Spread existing courses across the palette instead of leaving them all teal. Deterministic per course.
UPDATE courses
SET color = (ARRAY['TEAL', 'BLUE', 'INDIGO', 'VIOLET', 'ROSE', 'ORANGE', 'AMBER', 'SLATE'])[1 + mod(abs(hashtext(id::text)::bigint), 8)::int];
