# Notare

A tutoring management platform for AP Computer Science A (Java), with an embedded AI assistant
called **Sage** powered by the Anthropic Claude API.

## What it does

- Manage students, courses, sessions, and assignments
- Write raw session notes that Sage drafts into polished summaries
- Generate AI feedback on student Java submissions
- Gate all Sage feedback behind tutor approval before students can see it

## Stack

**Backend** — Java 21, Spring Boot 3.3, Spring Security + JWT, Spring Data JPA, Flyway, PostgreSQL, Maven
**Frontend** — Next.js 15, React 18, TypeScript, Tailwind CSS, shadcn/ui, TanStack Query
**AI** — Anthropic Java SDK, routed through a single `SageService`
**Hosting** — Railway (backend + database), Vercel (frontend)

## Status

Early development. Current phase: Spring Boot project setup.

## Local development

Requirements: JDK 21, Maven, PostgreSQL, Node.js 20+.

```bash
# Backend
mvn spring-boot:run

# Frontend (once scaffolded)
cd frontend && npm install && npm run dev
```

Configuration lives in `src/main/resources/application.yml`. Secrets (database URL, JWT secret,
Anthropic API key) are supplied via environment variables and are never committed.
