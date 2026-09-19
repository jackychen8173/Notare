# Admin Portal Design Spec

Date: 2026-09-18
Status: Approved by user, pending implementation plan

## Purpose

Notare currently has exactly two roles, `TUTOR` and `STUDENT`, and every tutor is fully
independent — students are scoped per-tutor via course enrollment (`StudentService`,
`CourseService`, etc. all filter by `authentication.getName()`), with no concept anywhere of a
user who can see across tutors. There is no operator-facing view of the platform at all: nobody
can list every account, see how many tutors/students/courses exist, or deactivate an account that
needs it. This spec adds a third role, `ADMIN`, and an admin-only portal for account oversight and
management, plus read access into course/assignment/submission content across every tutor.

## Scope decisions (from user Q&A during brainstorming)

- **Full platform control, not read-only**: account management (deactivate/reactivate) plus
  content oversight (courses, assignments, submissions, Sage feedback) across every tutor — not
  just an account directory.
- **Admin provisioning is seed/manual only.** No API path creates an `ADMIN` account, ever. The
  first (and every) admin is inserted directly into the database by a human. This closes a real
  pre-existing hole: `POST /api/auth/register` currently trusts whatever `role` value is in the
  request body verbatim (the frontend picklist only offers Teacher/Student, but the API itself
  doesn't restrict it), so without an explicit block, anyone could self-register as `ADMIN` by
  calling the API directly.
- **Soft deactivate only — no hard delete.** Originally scoped to include hard delete via
  `ON DELETE CASCADE` migrations, but the cascade migration was judged too risky (touches nearly
  every FK in the schema, none of which currently cascade — verified by grepping every
  `REFERENCES` across `db/migration/`) and dropped from scope entirely per user direction, not
  replaced with an application-level cascade either. Deactivation (`users.active = false`) is the
  only account-removal action this spec implements.
- **Admin's cross-tutor reads are parallel controllers, not a shared/extended path.** New
  `/api/admin/*` controllers with their own thin service methods that query repositories with no
  ownership filter, mirroring how `StudentCourseController` already sits alongside
  `CourseController` rather than extending it. Rejected alternative: adding `ADMIN` to every
  existing controller's `@PreAuthorize` and threading an admin-bypass branch through every
  ownership-check method (`requireVisibleStudent`, `requireOwnedCourse`, etc.) across ~10 modules —
  more reuse, but mixes two authorization models in code tutors currently depend on, raising
  regression risk for no benefit here.

## Data model

Two additive migrations, no destructive schema changes:

- **`V14__add_admin_role.sql`** — drop and recreate the `users.role` CHECK constraint
  (`CREATE TABLE users (... role VARCHAR(20) NOT NULL CHECK (role IN ('TUTOR', 'STUDENT')) ...)`
  from `V1`) to allow `'ADMIN'`. `UserRole` enum gains `ADMIN`.
- **`V15__add_user_active_flag.sql`** — `ALTER TABLE users ADD COLUMN active BOOLEAN NOT NULL
  DEFAULT true`. `User` entity gains `private boolean active = true;`.

No other tables change. No cascade behavior is added anywhere.

## Backend API

New `AdminController` family under `/api/admin/*`, class-level `@PreAuthorize("hasRole('ADMIN')")`,
following the existing controller-per-concern pattern:

| Endpoint | Purpose |
|---|---|
| `GET /api/admin/users?role=&active=` | List all tutors + students, optionally filtered |
| `GET /api/admin/users/{id}` | User detail, no ownership check |
| `PATCH /api/admin/users/{id}/deactivate` | Set `active = false` |
| `PATCH /api/admin/users/{id}/reactivate` | Set `active = true` |
| `GET /api/admin/courses` | Every course, any tutor |
| `GET /api/admin/courses/{id}` | Course detail + roster, no ownership check |
| `GET /api/admin/assignments/{id}` | Assignment detail, no ownership check |
| `GET /api/admin/submissions/{id}` | Full submission detail via the existing `SubmissionResponse.from()` factory (the tutor-facing one — includes Sage feedback/tutor feedback/grade, not the `forStudent()`-gated view; admin oversight means seeing everything, unlike a student) |
| `GET /api/admin/dashboard` | Platform counts: tutors, students, courses, submissions pending/released |

Each endpoint's service method is a direct repository query with no tutor/ownership filter — new,
small, admin-specific service methods, not reuse of the ownership-scoped methods on
`CourseService`/`StudentService`/etc.

### Closing the self-registration hole

`AuthService.register()` rejects `request.role() == ADMIN` outright (400/403), regardless of what
the client sends. This makes the "seed/manual only" provisioning decision actually enforced, not
just a frontend convention. The spec does not include a script or migration to create the first
admin row — that's a manual `INSERT` a human runs directly against the database, per the existing
harness rule that DB mutations are never run by Claude; the implementation plan should produce an
example statement for the user to run themselves, not execute it.

### Session invalidation on deactivation

`JwtAuthenticationFilter` gains an `active` check: after resolving the authenticated user, if
`active` is `false`, clear the security context (same handling as an invalid/expired token) so the
request is rejected with `401`. This means deactivating a user kills their live session on their
very next request, not just future login attempts — necessary because JWTs aren't otherwise
revocable in this app (no token blocklist exists or is being added here).

## Frontend

New `admin/` segment (a real path segment, not a route group — same reasoning as `student/` from
phase 12: route groups can't each own the same URL, and admin needs its own distinct paths):

- `app/admin/layout.tsx` — `useAuthGuard("ADMIN")` + the existing generalized `<Sidebar items=
  {...} />` component (already takes an `items` prop since phase 12, so no layout changes needed
  there).
- `app/admin/dashboard/page.tsx` — platform counts from `GET /api/admin/dashboard`.
- `app/admin/users/page.tsx`, `app/admin/users/[id]/page.tsx` — list/detail, with
  Deactivate/Reactivate actions behind a confirm `Dialog` (matching existing shadcn usage
  elsewhere), calling the new `PATCH` endpoints.
- `app/admin/courses/page.tsx`, `app/admin/courses/[id]/page.tsx` — list/detail, reusing existing
  display components (e.g. course roster rendering) where the DTO shape already matches.
- `app/admin/submissions/[id]/page.tsx` — reuses the existing submission display component, since
  `GET /api/admin/submissions/{id}` returns the same `SubmissionResponse` shape the tutor review
  screen already renders.
- Login redirect (`lib/auth.ts` / login page) gains a third branch: `role === "ADMIN"` →
  `/admin/dashboard`.
- New `hooks/useAdminUsers.ts`, `hooks/useAdminCourses.ts` (React Query), alongside the existing
  `useCourses`/`useAssignments` pattern.
- No register-page changes — `ADMIN` was never a selectable option there and stays that way.

## Explicitly out of scope for this spec

- **Admin-creating-admin** (an invite endpoint) — provisioning is seed/manual only; no UI or
  endpoint exists for one admin to create another.
- **Hard delete**, in any form (DB cascade or application-level cascade) — dropped per user
  direction mid-brainstorm; deactivation is the only removal action.
- **Editing course/assignment/submission content as admin** (grades, feedback text, rubric edits)
  — this spec is oversight (read) + account management (deactivate/reactivate), not a content
  editor. An admin can deactivate a tutor; they cannot rewrite that tutor's gradebook.
- **Audit logging of admin actions** (who deactivated whom, when) — no audit-log infrastructure
  exists anywhere else in the app either; adding one solely for this feature would be new
  infrastructure beyond what oversight requires here.
- Any change to existing tutor-scoped or student-scoped controllers/services — the parallel-
  controller approach means none of that code is touched.

## Testing / verification plan

Same standing limitation as phases 8–13: the backend can't run in this environment (no
Maven/JDK), so verification is build/lint plus reading the new code against the actual DTOs and
repository methods, not an end-to-end run. Specifically:

- `./mvnw compile` succeeds with the two new migrations and `ADMIN` role wired through
  `UserRole`/`SecurityConfig`/`AuthService`.
- Confirm `AuthService.register()` rejects an `ADMIN` role value by reading the added check against
  `RegisterRequest`/`AuthController`, since it can't be curl-tested live here.
- Confirm each new `Admin*Controller` method's repository query has no ownership filter (by
  reading it against the equivalent tutor-scoped method it deliberately does *not* reuse).
- Frontend: `npm run build` (all new admin routes resolve), `npm run lint`, and a live dev-server
  smoke test of `/admin/dashboard`, `/admin/users`, `/admin/users/[id]`, `/admin/courses`,
  `/admin/courses/[id]`, `/admin/submissions/[id]` for 200s — same pattern as every prior frontend
  phase's verification.
- Compiled-CSS spot check if any new admin-specific UI needs a token not already exercised
  elsewhere (unlikely — this reuses existing shadcn components throughout).
