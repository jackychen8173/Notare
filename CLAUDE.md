# Notare — Working Rules

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
