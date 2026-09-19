-- Finds the existing CHECK constraint on users.role (auto-named by Postgres,
-- e.g. users_role_check) dynamically rather than assuming its exact name,
-- since it was created via inline column-level CHECK syntax in V1.
DO $$
DECLARE
    existing_constraint text;
BEGIN
    SELECT con.conname INTO existing_constraint
    FROM pg_constraint con
    JOIN pg_class rel ON rel.oid = con.conrelid
    WHERE rel.relname = 'users'
      AND con.contype = 'c'
      AND pg_get_constraintdef(con.oid) LIKE '%role%';

    IF existing_constraint IS NOT NULL THEN
        EXECUTE format('ALTER TABLE users DROP CONSTRAINT %I', existing_constraint);
    END IF;
END $$;

ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role IN ('TUTOR', 'STUDENT', 'ADMIN'));
