#!/usr/bin/env bash
# Starts the disposable local Postgres container and runs the Spring Boot backend
# against it. See .claude/skills/run-locally/SKILL.md for the full breakdown.
set -e

docker start notare-local-pg

export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.7.6-hotspot"
export DATABASE_URL="jdbc:postgresql://localhost:55432/notare"
export DATABASE_USERNAME="notare"
export DATABASE_PASSWORD="changeme"
export JWT_SECRET="local-dev-secret-key-not-for-production-use-only-abcdefghijklmnop"
export CORS_ALLOWED_ORIGINS="http://localhost:3000"
# Required at startup (no defaults). Dummy values are enough to boot; real Google sign-in and
# code-run need the real values (see CLAUDE.md).
export GOOGLE_OAUTH_CLIENT_ID="local-dev-dummy.apps.googleusercontent.com"
export CODE_RUN_INTERNAL_SECRET="local-dev-dummy-secret"

./mvnw.cmd spring-boot:run
