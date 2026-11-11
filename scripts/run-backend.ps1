# Starts the disposable local Postgres container and runs the Spring Boot backend
# against it. See .claude/skills/run-locally/SKILL.md for the full breakdown.
$ErrorActionPreference = "Stop"

docker start notare-local-pg

# JDK 21.0.12.1 or later: older ones have BC's time zone wrong (permanent UTC-7 since 2026-11-01).
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\jdk-21.0.13+9"
$env:DATABASE_URL = "jdbc:postgresql://localhost:45432/notare"
$env:DATABASE_USERNAME = "notare"
$env:DATABASE_PASSWORD = "changeme"
$env:JWT_SECRET = "local-dev-secret-key-not-for-production-use-only-abcdefghijklmnop"
$env:CORS_ALLOWED_ORIGINS = "http://localhost:3000"
# Required at startup (no defaults). Dummy values are enough to boot; real Google sign-in and
# code-run need the real values (see CLAUDE.md).
$env:GOOGLE_OAUTH_CLIENT_ID = "local-dev-dummy.apps.googleusercontent.com"
$env:CODE_RUN_INTERNAL_SECRET = "local-dev-dummy-secret"

& .\mvnw.cmd spring-boot:run
