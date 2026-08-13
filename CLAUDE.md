# Notare — Working Rules

## Project spec

The full original project spec (tech stack, data model, API design, Sage feedback format, design system, folder structure, role-switching commands, coding standards, build order) lives in `docs/notare-system-prompt.md`, kept verbatim as originally written. Known deviations from it, made deliberately as the project progressed:

- **Spring Boot 4.1 / Java 21, not Spring Boot 3.3** — the entire 3.x line reached end-of-life in June 2026, see `CHANGELOG.md`. This changed some artifact/package names (`spring-boot-starter-webmvc`, explicit `spring-boot-starter-flyway`, per-technology test starters).
- A few classes exist beyond the spec's illustrative folder tree because they're structurally required, not optional: `NotareApplication.java` (entry point), `auth/JwtAuthenticationFilter.java` (JWT can't protect endpoints without a filter), and an `auth/dto/` subpackage for request/response DTOs (the spec's "DTOs for all request/response objects" rule needs somewhere to live).
- **Next.js 16 / React 19, not Next.js 15 / React 18** — 16 was the current stable major when phase 8 started (`npm view next version` → `16.3.0`; Next 16 requires React 19), and nothing had been built against 15 yet, so there was no migration cost to starting fresh on current. Brings Tailwind CSS v4 (CSS-first `@theme` config, not the `tailwind.config.js` the spec's era assumed) and the `middleware.ts` → `proxy.ts` rename (not yet relevant — no middleware/proxy exists yet).
- **shadcn/ui CLI defaults differ from its own docs** — `shadcn init -d` on the installed CLI version produced `--base base-ui` (Base UI primitives) and `style: "base-nova"`, not the Radix/`new-york` combination shown in most shadcn examples. Left as the CLI's actual current default rather than forcing Radix/new-york, since nothing in the spec requires either specifically.
- **Icon library: Tabler Icons (outline), not shadcn's default Lucide** — the spec's design system explicitly calls for Tabler. `components.json` still says `"iconLibrary": "lucide"` (shadcn's default, unedited) since no component has imported an icon yet; swap to `@tabler/icons-react` directly whenever the first icon is actually needed rather than fighting shadcn's icon-swapping machinery for an unused setting.

## Progress

Tracking against the spec's build order (`docs/notare-system-prompt.md`). See `CHANGELOG.md` for the detailed why behind each change.

1. ✅ Spring Boot setup + `pom.xml` — Spring Boot 4.1.0 / Java 21, not 3.3 (see "Project spec" above)
2. ✅ Flyway migrations — added incrementally per phase, not all at once (`V1__create_users.sql`, `V2__create_courses.sql`, `V3__create_sessions.sql`, `V4__create_assignments.sql`, `V5__create_submissions.sql` so far)
3. ✅ User entity + JWT auth — register/login, stateless JWT filter, Spring Security 7 config
4. ✅ Student + Course + Enrollment — tutor-scoped; no student-facing read access yet (deliberately deferred, spec doesn't define it)
5. ✅ Session + SessionNote — tutor-scoped, same 404-not-403 ownership pattern as Course; `save_session_notes` upserts a single `SessionNote` per `Session`
6. ✅ Assignment + Submission — Assignment reads/writes are tutor-scoped like Course; `submit_assignment` is the first STUDENT-role endpoint and checks course enrollment; `release_feedback` now derives APPROVED vs REVISED from whether the tutor added their own commentary (retroactively refined once Sage gave the distinction meaning)
7. ✅ SageService + Sage endpoints — all four Sage endpoints are manual/tutor-triggered (no auto-trigger wired into `submit_assignment` — kept phase 6 untouched, deliberately deferred per the "don't invent unstated behavior" precedent); model is `claude-sonnet-4-6` exactly as the spec names it (still an actively-supported model, so no EOL-style deviation applies)
8. ✅ Next.js setup + design tokens — Next.js 16 / React 19 / Tailwind v4 (see "Project spec" above); `frontend/` scaffolded with `create-next-app`, shadcn/ui initialized, Inter + JetBrains Mono wired via `next/font` (weights 400/500 only, per spec), full color/radius/border token system encoded in `app/globals.css` matching the spec's design system section exactly (sage tokens explicitly commented as AI-only, `border-hairline` utility for the 0.5px-border rule, `rounded-card` for the 12px card radius, base `--radius` set to 6px so shadcn's own `rounded-lg` already matches the button radius). React Query, React Hook Form + Zod, FullCalendar, and Recharts are **not installed yet** — deferred to whichever phase first uses them, matching the backend's incremental-not-batch precedent. Actually built and verified this time: Node 24 / npm 11 are available locally (unlike the backend's missing Maven/JDK), so `npm run build`, `npm run lint`, and a live `npm run dev` request all passed, and the compiled CSS was inspected directly to confirm the custom tokens (`border-hairline`, `rounded-card`, `bg-sage-surface`, etc.) actually resolved.
9. ⬜ Layout shell (sidebar + nav) — **next up**
10. ⬜ Auth pages
11. ⬜ Tutor screens
12. ⬜ Student screens
13. ⬜ Deploy (Railway + Vercel)

**State a new session should know:**
- Backend (`src/`) and a bare-bones `frontend/` (Next.js scaffold + design tokens only, no real pages yet) both exist now.
- No Maven or JDK is installed in this dev environment, so none of the Java backend has actually been compiled or run — everything there was hand-reviewed against current docs instead. Worth installing both (or confirming IntelliJ's bundled JDK/Maven can be used) before trusting a build, and definitely before deploying.
- Node 24 / npm 11 **are** available in this dev environment, so the frontend can actually be built/linted/run — `cd frontend && npm run build` / `npm run dev` both work today, unlike the backend.
- No real database has been provisioned yet (no Railway Postgres instance), so migrations have never actually been applied — they've only been reviewed by hand, per the DB safety harness above.
- Everything is pushed to `main` on `jackychen8173/Notare` through commit `aa27056`; the frontend scaffold from this session is not yet committed.

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
