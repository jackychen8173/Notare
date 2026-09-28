-- Accounts created by the landing page's one-click demo ("Try it as a teacher/student"). Each click
-- creates its own private demo teacher + student with seeded sample content. Demo accounts are
-- excluded from the admin portal's lists and counts, and can't use Sage or the code Run button
-- (both cost money per call). Rows are kept, not cleaned up.
ALTER TABLE users ADD COLUMN demo BOOLEAN NOT NULL DEFAULT false;
