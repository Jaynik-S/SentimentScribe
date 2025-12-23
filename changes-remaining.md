## 1) Replace the current “password gate” with real per-user auth (Spring Security + JWT + BCrypt)

- [ ] Add user registration + login (and remove the single `default` user concept)
  - Store `users.password` as a BCrypt hash (never plaintext).
  - Decide minimal user fields (e.g., `id`, `username/email`, `password_hash`, timestamps).
- [ ] Implement stateless JWT authentication (access tokens only is enough)
  - Issue JWTs on login (and optionally on register).
  - Add Spring Security filter chain to validate `Authorization: Bearer <token>`.
  - Wire “current user” into controllers/services (e.g., via `Authentication` / `@AuthenticationPrincipal`).
- [ ] Enforce per-user authorization for diary endpoints
  - Every entry read/write/delete must be scoped to the authenticated user.
  - Update repository queries to include `user_id` checks (no cross-user access).
- [ ] Update the frontend auth flow
  - Replace `/api/auth/verify` UI with login/register screens.
  - Attach access token on API requests.
  - Handle token expiration (minimal: force logout + re-login).

## 2) End-to-end encryption for stored diary entries (backend stores ciphertext only)

- [ ] Decide the minimal E2EE design (one that works with your current features)
  - Encrypt/decrypt on the client; backend stores only encrypted blobs + metadata.
  - Keep NLP + recommendations by analyzing plaintext *on demand* (client sends plaintext to `/api/analysis` + `/api/recommendations`, but entries persisted remain encrypted).
- [ ] Update the database schema + migrations for encrypted entry storage
  - Replace `diary_entries.title/text` plaintext columns with fields like:
    - `ciphertext`, `iv/nonce`, `version`, `algo`, `content_type`, `updated_at`, etc.
  - Optional “just enough” metadata to still list entries: store *encrypted title* or store a user-chosen non-sensitive label.
- [ ] Update backend entry DTOs + persistence adapter
  - Accept/return encrypted payloads instead of plaintext entry bodies.
  - Stop server-side keyword extraction from persisted data (it won’t have plaintext anymore).
- [ ] Add key management UX rules (minimum viable)
  - Derive an encryption key from a passphrase (Web Crypto PBKDF2 is “just enough”).
  - Never send the raw key/passphrase to the backend.
  - Keep decrypted keys in memory only while “unlocked”.

## 3) Implement on-device encryption in the React app (Web Crypto API)

- [ ] Create a small crypto module for: deriveKey → encrypt → decrypt
  - AES-GCM encryption with random per-entry IV.
  - Version the encrypted payload so you can migrate later.
- [ ] Update the entry editor flow
  - Encrypt before save; decrypt after load.
  - Ensure plaintext never gets written to backend persistence endpoints.
- [ ] Add “lock/unlock” behavior (separate from backend auth)
  - “Unlock diary” prompts for passphrase (local-only).
  - Clear decrypted keys on logout/lock/tab close (best-effort).

## 4) Offline-first support using IndexedDB (sync encrypted entries when online)

- [ ] Add an IndexedDB store for encrypted entries + a minimal sync queue
  - Store encrypted entries locally immediately.
  - Track “dirty” entries and pending deletes.
- [ ] Implement sync to backend when online
  - Push local changes (ciphertext) to the server; pull remote changes (if multi-device is in scope).
  - Decide conflict strategy (MVP: “last write wins” using `updated_at`).
- [ ] Make the UI resilient offline
  - Home list + entry load should work from IndexedDB even if the API is unreachable.
  - Show a simple “Offline” indicator + “Sync pending” count (optional, but nice).

## 5) Docker + AWS deployment (EC2 backend container, S3/CloudFront frontend, config via SSM)

- [ ] Containerize the backend (and optionally the frontend for local prod-parity)
  - Add `backend/Dockerfile` to build and run the Spring Boot jar.
  - Expand `docker-compose.yml` to include `backend` + `postgres` (and `frontend` if desired).
  - Ensure Flyway migrations run on startup in the container environment.
- [ ] Deploy backend container on EC2 (minimal but real)
  - Build image locally or in CI, ship to EC2 (or push to ECR).
  - Run with Docker on EC2; secure ports (only 80/443 public; app port private).
  - Configure env vars/secrets from AWS SSM Parameter Store (DB creds, JWT secret, Spotify/TMDb keys, CORS origins).
- [ ] Deploy frontend to S3 + CloudFront
  - Build with `VITE_API_BASE_URL` pointing at the deployed backend URL.
  - Upload `frontend/dist` to S3, serve via CloudFront.
  - (Recommended) Add a custom domain + TLS (ACM) and set correct caching/invalidations.
- [ ] Production hardening “just enough”
  - Set strict CORS to your CloudFront domain.
  - Add HTTPS termination (ALB or reverse proxy like Nginx/Caddy, or CloudFront → API Gateway/ALB if you go that route).
  - Store secrets only in SSM/Secrets Manager (no default keys in repo config).

