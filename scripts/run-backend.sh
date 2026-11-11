#!/usr/bin/env bash
# Starts the disposable local Postgres container and runs the Spring Boot backend
# against it. See .claude/skills/run-locally/SKILL.md for the full breakdown.
set -e

docker start notare-local-pg

# JDK 21.0.12.1 or later: older ones have BC's time zone wrong (permanent UTC-7 since 2026-11-01).
export JAVA_HOME="$HOME/.jdks/jdk-21.0.13+9"
export DATABASE_URL="jdbc:postgresql://localhost:45432/notare"
export DATABASE_USERNAME="notare"
export DATABASE_PASSWORD="changeme"
export JWT_SECRET="local-dev-secret-key-not-for-production-use-only-abcdefghijklmnop"
export CORS_ALLOWED_ORIGINS="http://localhost:3000"
# Required at startup (no defaults). Dummy values are enough to boot; real Google sign-in and
# code-run need the real values (see CLAUDE.md).
export GOOGLE_OAUTH_CLIENT_ID="local-dev-dummy.apps.googleusercontent.com"
export CODE_RUN_INTERNAL_SECRET="local-dev-dummy-secret"

./mvnw.cmd spring-boot:run
