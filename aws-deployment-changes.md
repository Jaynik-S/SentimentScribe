# AWS Deployment — Codex Change Log Template

Use this file as the running “paper trail” of **repo changes Codex makes** while executing the deployment plan.

Rule: after each **CODEX DOES THIS** step is implemented, Codex must fill in the corresponding section below.

---

## Codex Step 1 - Add production backend Dockerfile
- Files changed:
- `backend/Dockerfile`
- `backend/.dockerignore`
- Summary:
- Added a multi-stage Docker build that packages the Spring Boot app into a runnable jar and runs it on a small JRE base image.
- Added a backend `.dockerignore` to keep image build contexts small (excludes `target/` and IDE artifacts).
- How to run/verify:
- `docker build -t sentimentscribe-backend:local -f backend/Dockerfile backend`
- `docker run --rm -p 8080:8080 sentimentscribe-backend:local` (provide required env vars for DB/API keys as needed)

## Codex Step 2 - Add/adjust prod-parity Docker Compose
- Files changed:
- `docker-compose.prod.yml`
- Summary:
- Added a prod-parity compose file that runs `backend` + `postgres` with no code bind-mounts and persists Postgres data via a named volume.
- Backend config is passed via environment variables (DB/JWT/API keys/CORS) with safe defaults for local use.
- How to run/verify:
- `docker compose -f docker-compose.prod.yml up -d --build`
- `docker compose -f docker-compose.prod.yml logs -f postgres`
- `docker compose -f docker-compose.prod.yml logs -f backend`
- `curl http://localhost:8080/api/health`
- `docker compose -f docker-compose.prod.yml down` then re-run `up -d` and confirm DB volume persists (`sentiment-scribe_pgdata`).

## Codex Step 3 — Add EC2 production compose + deploy helpers
- Files changed:
- Summary:
- How to run/verify:

## Codex Step 4 — Make production Postgres persistence + health explicit
- Files changed:
- Summary:
- How to run/verify:

## Codex Step 5 — Make backend config SSM-friendly (env + CORS list)
- Files changed:
- Summary:
- How to run/verify:

## Codex Step 6 — Add SSM-to-env rendering script for EC2
- Files changed:
- Summary:
- How to run/verify:

## Codex Step 7 — Add Caddy reverse proxy config + compose wiring
- Files changed:
- Summary:
- How to run/verify:

## Codex Step 8 — Frontend: confirm/provide production API base wiring
- Files changed:
- Summary:
- How to run/verify:

## Codex Step 9 — Backend: finalize production CORS allowlist for domains
- Files changed:
- Summary:
- How to run/verify:

## Codex Step 10 — “Just enough” production hardening (headers/logging)
- Files changed:
- Summary:
- How to run/verify:

