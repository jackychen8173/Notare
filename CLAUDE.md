# Notare — Working Rules

## Project spec

The full original project spec (tech stack, data model, API design, Sage feedback format, design system, folder structure, role-switching commands, coding standards, build order) lives in `docs/notare-system-prompt.md`, kept verbatim as originally written. Known deviations from it, made deliberately as the project progressed:

- **Spring Boot 4.1 / Java 21, not Spring Boot 3.3** — the entire 3.x line reached end-of-life in June 2026, see `CHANGELOG.md`. This changed some artifact/package names (`spring-boot-starter-webmvc`, explicit `spring-boot-starter-flyway`, per-technology test starters).
- A few classes exist beyond the spec's illustrative folder tree because they're structurally required, not optional: `NotareApplication.java` (entry point), `auth/JwtAuthenticationFilter.java` (JWT can't protect endpoints without a filter), and an `auth/dto/` subpackage for request/response DTOs (the spec's "DTOs for all request/response objects" rule needs somewhere to live).

## Progress

Tracking against the spec's build order (`docs/notare-system-prompt.md`). See `CHANGELOG.md` for the detailed why behind each change.

1. ✅ Spring Boot setup + `pom.xml` — Spring Boot 4.1.0 / Java 21, not 3.3 (see "Project spec" above)
2. ✅ Flyway migrations — added incrementally per phase, not all at once (`V1__create_users.sql`, `V2__create_courses.sql`, `V3__create_sessions.sql`, `V4__create_assignments.sql`, `V5__create_submissions.sql` so far)
3. ✅ User entity + JWT auth — register/login, stateless JWT filter, Spring Security 7 config
4. ✅ Student + Course + Enrollment — tutor-scoped; no student-facing read access yet (deliberately deferred, spec doesn't define it)
5. ✅ Session + SessionNote — tutor-scoped, same 404-not-403 ownership pattern as Course; `save_session_notes` upserts a single `SessionNote` per `Session`
6. ✅ Assignment + Submission (no Sage yet) — Assignment reads/writes are tutor-scoped like Course; `submit_assignment` is the first STUDENT-role endpoint and checks course enrollment; `release_feedback` sets `feedback_status=APPROVED` since there's no Sage output yet to mark REVISED
7. ⬜ SageService + Sage endpoints — **next up**
8. ⬜ Next.js setup + design tokens
9. ⬜ Layout shell (sidebar + nav)
10. ⬜ Auth pages
11. ⬜ Tutor screens
12. ⬜ Student screens
13. ⬜ Deploy (Railway + Vercel)

**State a new session should know:**
- Backend only so far — no `frontend/` directory exists yet (phase 8+).
- No Maven or JDK is installed in this dev environment, so nothing here has actually been compiled or run — everything was hand-reviewed against current docs instead. Worth installing both (or confirming IntelliJ's bundled JDK/Maven can be used) before trusting a build, and definitely before deploying.
- No real database has been provisioned yet (no Railway Postgres instance), so migrations have never actually been applied — they've only been reviewed by hand, per the DB safety harness above.
- Everything is pushed to `main` on `jackychen8173/Notare` through commit `0f38594`.

## Safety harnesses (always enforced, in every permission mode including auto mode)

1. **No file deletion without confirmation.** `rm`, `git rm`, `git clean`, `Remove-Item`, `rmdir`, `del`, and similar always require an explicit confirmation prompt.
2. **Every `git push` requires explicit confirmation.** Same enforcement, no exceptions.
3. **Database changes are done by a human, never by Claude.** Direct DB CLIs (`psql`, `mysql`, `mongosh`, `sqlcmd`, `sqlite3`, `redis-cli`) and mutating Flyway/Liquibase commands (`migrate`, `clean`, `repair`, `undo`, `baseline`, `update`, `rollback`, `dropAll`) are hard-blocked outright — Claude cannot run them at all, confirmation or not. Claude may still author migration SQL files (e.g. `V1__create_users.sql`) for the user to review and apply themselves; read-only inspection commands like `flyway:validate`/`flyway:info` are not blocked.
   - Caveat: starting the app locally (`mvn spring-boot:run`) still triggers Flyway's normal auto-migration against whatever database it's configured against — that's Spring Boot's own startup behavior, not a command Claude runs directly, so this harness doesn't catch it. Point local dev at a disposable/local database if you want to avoid that too.

Enforced by `.claude/settings.json` (`permissions.deny`/`permissions.ask`, plus `autoMode.hard_deny` for the built-in auto-mode classifier) and the `.claude/hooks/guard-destructive.js` PreToolUse hook, which scans full command text so compound commands (`docker exec db psql ...`, `build && git push`) are still caught, not just literal prefixes. These three rules hold in every permission mode — default, auto, accept-edits — because they're enforced mechanically, not left to in-the-moment judgment.

## Auto mode

Toggle Claude Code's built-in "auto" permission mode (`/config` → permission mode, or Shift+Tab to cycle) to let Claude run routine local dev work — builds, tests, lint, `git status`/`diff`/`log`/`add`/`commit`, file edits — without a prompt each time. The three rules above still apply in every mode, including auto.

## Changelog

After any non-trivial change (new dependency, schema change, new endpoint, architectural decision, version bump with a reason), add a dated entry to `CHANGELOG.md` summarizing what changed and why. Keep entries terse — a couple of lines, not a diff dump.
