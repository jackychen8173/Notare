# Starts the disposable local Postgres container and runs the Spring Boot backend
# against it. See .claude/skills/run-locally/SKILL.md for the full breakdown.
$ErrorActionPreference = "Stop"

docker start notare-local-pg

$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.7.6-hotspot"
$env:DATABASE_URL = "jdbc:postgresql://localhost:55432/notare"
$env:DATABASE_USERNAME = "notare"
$env:DATABASE_PASSWORD = "changeme"
$env:JWT_SECRET = "local-dev-secret-key-not-for-production-use-only-abcdefghijklmnop"
$env:CORS_ALLOWED_ORIGINS = "http://localhost:3000"

& .\mvnw.cmd spring-boot:run
