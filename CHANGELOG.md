# Changelog

Notable changes to this project, most recent first. See `CLAUDE.md` for when to add an entry.

## 2026-08-12

- Added safety harnesses: file deletion and `git push` now always require explicit confirmation, and direct database commands (DB CLIs, mutating Flyway/Liquibase commands) are hard-blocked for Claude entirely. Enforced via `.claude/settings.json` permission rules plus a PreToolUse hook (`.claude/hooks/guard-destructive.js`) that scans full command text so compound/piped commands are caught too, not just literal prefixes. Documented in `CLAUDE.md`.
- Set up `pom.xml` for Spring Boot 4.1.0 / Java 21. Originally scoped for Spring Boot 3.3, but the entire 3.x line reached end-of-life in June 2026 (final release 3.5.16), so moved to the actively-supported 4.x line instead. This changed several artifact names: `spring-boot-starter-web` → `spring-boot-starter-webmvc`, Flyway now needs an explicit `spring-boot-starter-flyway` starter (not just `flyway-core`), and test dependencies are now per-technology (`spring-boot-starter-webmvc-test`, `-data-jpa-test`, `-security-test`) instead of one monolithic `spring-boot-starter-test`.
- Initialized the git repository and created the GitHub remote at `jackychen8173/Notare`.
