---
name: run-locally
description: Start Notare's backend (Spring Boot) and frontend (Next.js) locally for testing, against a disposable local Postgres. Use when asked to run, start, or test the app locally.
---

# Running Notare locally

Three pieces: local Postgres (Docker), the Spring Boot backend, the Next.js frontend.

## 1. Local Postgres (Docker Desktop must be running)

A disposable container `notare-local-pg` already exists from prior sessions, mapped to host port **55432** (not the default 5432):

```bash
docker start notare-local-pg
```

If it doesn't exist yet (fresh machine), create it:

```bash
docker run -d --name notare-local-pg \
  -e POSTGRES_USER=notare -e POSTGRES_PASSWORD=changeme -e POSTGRES_DB=notare \
  -p 55432:5432 postgres:16
```

Credentials: user `notare` / password `changeme` / db `notare`.

## 2. Backend (Spring Boot, JDK 21)

The system default JDK is 19 — must override `JAVA_HOME` to the JDK 21 install for this repo (per `CLAUDE.md`, Spring Boot 4.1 requires Java 21):

```bash
export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.7.6-hotspot"
export DATABASE_URL="jdbc:postgresql://localhost:55432/notare"
export DATABASE_USERNAME="notare"
export DATABASE_PASSWORD="changeme"
export JWT_SECRET="local-dev-secret-key-not-for-production-use-only-abcdefghijklmnop"
export CORS_ALLOWED_ORIGINS="http://localhost:3000"
./mvnw.cmd spring-boot:run
```

Runs on `http://localhost:8080`. Flyway auto-migrates on startup (harmless against this disposable local DB — the repo-wide "no DB mutation" safety harness is about production/shared databases). Swagger UI at `/swagger-ui.html`.

Sanity check once it's up:
```bash
curl -s http://localhost:8080/v3/api-docs -o /dev/null -w "%{http_code}\n"   # expect 200
```

## 3. Frontend (Next.js)

```bash
cd frontend
npm run dev
```

Runs on `http://localhost:3000`. Needs `frontend/.env.local` (gitignored):
```
NEXT_PUBLIC_API_URL=http://localhost:8080
```
Copy from `frontend/.env.example` if missing.

## Notes

- Backend takes ~6s to boot (Flyway validate + Tomcat start). Frontend (Turbopack) is near-instant.
- Both are long-running processes — start each in the background (or separate terminals) rather than sequentially in one shell.
- A real end-to-end check beyond route 200s: `POST http://localhost:8080/api/auth/register` with a JSON body (`email`, `password`, `name`, `role: "TUTOR"`) should return `201` with a JWT — confirms backend, DB, and migrations are all actually wired together, not just that the process launched.
