Technical Changes:
- (1) Spring Boot App (entrypoint, expose REST endpoints) **(DONE)**
  - Baseline API routes exist; keep future changes backward-compatible where possible.
  - Add consistent request validation + error responses as new endpoints evolve.
- (2) React Front End **(DONE)**
  - Keep API integration points centralized (e.g., one client/service layer) as auth + new endpoints land.


- (3) Replace file-based storage with PostgreSQL
  - Define the initial schema (users, diary entries metadata).
  - Add migrations (Flyway) so schema changes are repeatable across dev/staging/prod.
  - Implement the persistence layer (JPA repositories/services) and a migration plan for any existing file data (if needed).

- (4) Auth + authorization (Spring Security, JWT, BCrypt)
  - Store users in Postgres; hash passwords with BCrypt; never store plaintext passwords.
  - Implement JWT access tokens (and refresh tokens if you want longer sessions) + protected routes on the backend.
  - Add authorization rules for diary entries and shareable links (private vs public vs “unlisted”).
  - Update the React client to attach tokens, handle expiry/logout, and guard routes.

- (5) Containerization + local prod-parity (Docker)
  - Add Dockerfiles for backend and frontend; add `docker-compose` for local (frontend + backend + Postgres).
  - Centralize config via env vars (DB URL, JWT secret, CORS origins) and keep secrets out of git.
  - Ensure migrations run on startup (or as a separate migration step) in containerized environments.

- (6) Deployment (AWS S3 + backend hosting + managed database)
  - Host the React build on S3 (typically behind CloudFront) and wire environment-specific API base URLs.
  - Deploy the backend container (ECS/Fargate, Elastic Beanstalk, or EC2) and use a managed Postgres (RDS) for production.
  - Plan TLS, domain/DNS, and secret management (SSM/Secrets Manager) before going live.

- (7) Post-deploy hardening (recommended follow-up)
  - Add basic observability (structured logs, request tracing/correlation IDs, metrics/health checks).
  - Add security controls: rate limiting for auth endpoints, secure headers, and safer JWT key rotation strategy.
  - Add backups/retention for Postgres and an approach for DB migrations during deployments.

Feats:
- Secure Username + Password Login System
  - Backend: POST /auth/register, POST /auth/login, JWT issuance, protected diary endpoints; password hashing with BCrypt.
  - Data: users table + relationships to entries; store password hash + minimal profile fields.
  - Frontend: auth screens + token storage strategy + route guards + logout/expiry handling.
  - Ops: rate limit auth endpoints, basic audit logging, secure secret management for JWT keys.

- Local-first, On-device Encrypted Diary Entries
  - What it is: diary text is encrypted in the React app before it’s stored or synced, so the backend stores only ciphertext (not readable diary content).
  - Value: protects highly personal data, reduces breach impact, and builds user trust with a “zero-knowledge” storage model.
  - Implementation outline:
    - Frontend (React): encrypt/decrypt on-device (Web Crypto) and store encrypted entries locally (IndexedDB) for offline-first behavior.
    - Data storage: sync encrypted entry blobs + minimal metadata (IDs, timestamps, versions) to Postgres via Spring Boot without ever sending plaintext.
    - Key management: derive an “unlock” key from a user passphrase to decrypt a stored data-encryption key; keep decrypted keys in-memory only while unlocked.
    - API changes: entry create/read/update endpoints accept/return encrypted payloads (ciphertext + nonce/metadata) instead of plaintext bodies.
  - Constraints & tradeoffs:
    - Harder/impossible: server-side full-text search/analytics and server-side LLM chat over diary content (unless users explicitly choose to share plaintext per request).
    - Added complexity/risks: passphrase UX, recovery limitations (lost passphrase can mean unrecoverable data), and more complex multi-device sync.
    - Acceptable for now: personal-project scale + privacy-first positioning; client-side search can cover early needs.
  - Positioning (learning/resume):
    - Demonstrates applied security, key management, and privacy-aware architecture beyond standard CRUD.
    - Fits the roadmap after auth + Postgres by keeping the backend simple (stores blobs) while making the client more capable.

Other:
- UI Change
