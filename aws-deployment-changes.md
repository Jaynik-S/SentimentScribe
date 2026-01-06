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

## Codex Step 3 - Add EC2 production compose + deploy helpers
- Files changed:
- `deploy/ec2/docker-compose.prod.yml`
- `deploy/ec2/render-env-from-ssm.sh`
- `deploy/ec2/up.sh`
- `.gitignore`
- Summary:
- Added an EC2-focused production compose that runs `postgres` privately (no published ports) with a host-mounted data dir at `~/sentimentscribe/data/postgres`.
- Backend is configured to pull the image from ECR and is only reachable on a dedicated reverse-proxy Docker network (for Phase 6).
- Added helper scripts to render `.env.prod` from SSM and bring the stack up safely.
- How to run/verify:
- On EC2, `cd` to the repo's `deploy/ec2` directory and `chmod +x *.sh`.
- Create `.env.prod` (or render it later from SSM), then run `./up.sh`.

## Codex Step 4 - Make production Postgres persistence + health explicit
- Files changed:
- `deploy/ec2/docker-compose.prod.yml`
- Summary:
- Set `POSTGRES_DB` explicitly to `sentiment_scribe` in the EC2 compose and require `POSTGRES_USER`/`POSTGRES_PASSWORD` from `.env.prod`.
- Ensured the backend container uses the in-network JDBC URL and receives DB credentials via env vars.
- How to run/verify:
- On EC2, ensure `.env.prod` defines `POSTGRES_USER` and `POSTGRES_PASSWORD`.
- `docker compose -f deploy/ec2/docker-compose.prod.yml --env-file deploy/ec2/.env.prod config` to confirm interpolation.

## Codex Step 5 - Make backend config SSM-friendly (env + CORS list)
- Files changed:
- `backend/src/main/resources/application.yml`
- `backend/src/main/java/com/sentimentscribe/config/CorsProperties.java`
- `backend/src/main/java/com/sentimentscribe/config/WebConfig.java`
- `deploy/ec2/docker-compose.prod.yml`
- `docker-compose.prod.yml`
- Summary:
- Added `SENTIMENTSCRIBE_CORS_ORIGINS` (comma-separated) support and kept the existing two-origin env vars as a fallback.
- EC2 compose now passes CORS env vars through to the backend; the repo env example documents the single-var option.
- How to run/verify:
- Set `SENTIMENTSCRIBE_CORS_ORIGINS=https://app.sentimentscribe.cloud,https://www.sentimentscribe.cloud` and confirm CORS allows those origins.

## Codex Step 6 — Add SSM-to-env rendering script for EC2
- Files changed:
- Summary:
- How to run/verify:

## Codex Step 7 - Add Caddy reverse proxy config + compose wiring
- Files changed:
- `deploy/ec2/Caddyfile`
- `deploy/ec2/docker-compose.prod.yml`
- Summary:
- Added a Caddy reverse proxy config for `api.sentimentscribe.cloud`.
- Wired a `caddy` service into the EC2 compose file with ports 80/443 and persistent config/data volumes.
- How to run/verify:
- On EC2, ensure DNS for `api.sentimentscribe.cloud` points at the instance EIP.
- `docker compose -f deploy/ec2/docker-compose.prod.yml --env-file deploy/ec2/.env.prod up -d`
- `curl -I https://api.sentimentscribe.cloud/api/health`

## Codex Step 8 - Frontend: confirm/provide production API base wiring
- Files changed:
- `README.md`
- `README-frontend.md`
- Summary:
- Confirmed the frontend uses `VITE_API_BASE_URL` for API calls and documented production build usage.
- How to run/verify:
- `cd frontend`
- `VITE_API_BASE_URL=https://api.sentimentscribe.cloud npm run build` (PowerShell variant in README)

## Codex Step 9 - Backend: finalize production CORS allowlist for domains
- Files changed:
- `.env.example`
- Summary:
- Set the production CORS allowlist example to include `https://app.sentimentscribe.cloud` plus an optional CloudFront domain placeholder.
- How to run/verify:
- Update `.env.prod` with your real CloudFront domain and restart the stack (`./up.sh`), then verify no CORS errors in the browser.

## Codex Step 10 - "Just enough" production hardening (headers/logging)
- Files changed:
- `backend/src/main/java/com/sentimentscribe/config/SecurityConfig.java`
- `backend/src/main/resources/application.yml`
- Summary:
- Added basic security headers (HSTS, nosniff, Referrer-Policy) at the Spring Security layer.
- Disabled stack trace/message leakage in error responses via `server.error` settings.
- How to run/verify:
- Call `https://api.sentimentscribe.cloud/api/health` and confirm response headers include HSTS and `X-Content-Type-Options: nosniff`.

