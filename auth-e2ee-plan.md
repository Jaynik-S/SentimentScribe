# Auth + E2EE Implementation Plan (SentimentScribe)

> Change documentation rule (MANDATORY): after completing **each** step in this plan, document what you did under the matching section in `auth-e2ee-changes.md` (files changed, summary, architecture notes, verification).

## Table of Contents

- [0) Current State Inventory (repo-specific)](#0-current-state-inventory-repo-specific)
- [1) Target Architecture Overview](#1-target-architecture-overview)
- [2) API Contract Map (critical)](#2-api-contract-map-critical)
- [3) Database + Flyway migrations (critical)](#3-database--flyway-migrations-critical)
- [4) Step-by-step implementation plan (execution-ready)](#4-step-by-step-implementation-plan-execution-ready)
- [5) Security & privacy notes](#5-security--privacy-notes)
- [6) Testing plan](#6-testing-plan)
- [7) Rollout order + risk control](#7-rollout-order--risk-control)

---

## 0) Current State Inventory (repo-specific)

### 0.1 Current auth approach (“password gate”)

**Backend**
- Endpoint: `POST /api/auth/verify`
  - Controller: `backend/src/main/java/com/sentimentscribe/web/AuthController.java` (`verifyPassword(AuthRequest)`)
  - Service: `backend/src/main/java/com/sentimentscribe/service/AuthService.java` (`verifyPassword(String)`)
  - Persistence: `backend/src/main/java/com/sentimentscribe/persistence/postgres/PostgresVerifyPasswordDataAccessObject.java`
    - Hardcoded identity: `DEFAULT_USERNAME = "default"`
    - Stores **plaintext** password in `users.password`

**Frontend**
- Page: `frontend/src/pages/VerifyPasswordPage.tsx` calls `frontend/src/api/auth.ts#verifyPassword`
- Gate: `frontend/src/routes.tsx` uses `RequireUnlocked` + `frontend/src/state/auth.tsx` (`sessionStorage` key `sentimentscribe.isUnlocked`)

**What’s missing vs target**
- No registration/login, no Spring Security, no JWT, no BCrypt, no per-user scoping.

### 0.2 Current diary storage model (plaintext)

**DB schema**
- Flyway: `backend/src/main/resources/db/migration/V1__init.sql`
  - `users(id, username, password, created_at, updated_at)`
  - `diary_entries(id, user_id, storage_path, title, text, created_at, updated_at)`

**Entities**
- `backend/src/main/java/com/sentimentscribe/persistence/postgres/entity/UserEntity.java` stores plaintext `password`
- `backend/src/main/java/com/sentimentscribe/persistence/postgres/entity/DiaryEntryEntity.java` stores plaintext `title` + `text`

**REST (plaintext DTOs)**
- `backend/src/main/java/com/sentimentscribe/web/EntriesController.java`
  - `EntryRequest` / `EntryResponse` / `EntrySummaryResponse` contain plaintext `title` + `text`
- Repository adapter: `backend/src/main/java/com/sentimentscribe/persistence/postgres/PostgresDiaryEntryRepositoryAdapter.java`
  - also hardcodes `DEFAULT_USERNAME` and is not user-scoped (`findByStoragePath`)

### 0.3 Current analysis + recommendation flow

**Analysis**
- `POST /api/analysis` (`backend/src/main/java/com/sentimentscribe/web/AnalysisController.java`) accepts `{ "text": string }` and does not persist.
- Frontend calls it from `frontend/src/pages/DiaryEntryPage.tsx` with plaintext `title + "\n\n" + text`.

**Recommendations**
- `POST /api/recommendations` (`backend/src/main/java/com/sentimentscribe/web/RecommendationsController.java`) accepts `{ "text": string }` and does not persist.
- Frontend calls it from `frontend/src/pages/DiaryEntryPage.tsx` with plaintext `title + "\n\n" + text`.

---

## 1) Target Architecture Overview

### 1.1 Text architecture diagram

```
React (frontend/)
  - Register/Login (username + password) ───────────────┐
  - Stores JWT access token (stateless)                 │
  - Unlock (passphrase → PBKDF2 → AES-GCM key)          │  (key stays in-memory only)
  - Diary CRUD:
      encrypt before save → send ciphertext →           │
      receive ciphertext → decrypt after load           │
  - NLP + Recommendations:
      send plaintext to /api/analysis + /api/recommendations on demand (no persistence)
                                                       ▼
Spring Boot (backend/)
  - Public: POST /api/auth/register, POST /api/auth/login, GET /api/health
  - Protected (JWT): /api/entries/**, /api/analysis, /api/recommendations
  - Stores diary entries as ciphertext only (cannot decrypt stored entries)
                                                       ▼
Postgres
  - users: BCrypt password_hash + E2EE KDF params (salt/iterations)
  - diary_entries: ciphertext fields + IVs + algo/version + user_id scoping
```

### 1.2 Trust boundaries (explicit)

- Backend must never receive or store the E2EE passphrase or derived key.
- Backend must store **only ciphertext** for diary entries.
- Backend may receive plaintext **only** for compute-only endpoints (`/api/analysis`, `/api/recommendations`) and must not persist that plaintext.
- E2EE passphrase should be distinct from the login password (recommended). If you choose to reuse it, the backend would know it, weakening the “zero-knowledge” story.

### 1.3 Public vs protected endpoints

**Public**
- `POST /api/auth/register` (new)
- `POST /api/auth/login` (new)
- `GET /api/health` (existing)

**Protected (requires `Authorization: Bearer <accessToken>`; stateless)**
- `GET /api/entries`
- `GET /api/entries/by-path?path=...`
- `POST /api/entries`
- `PUT /api/entries`
- `DELETE /api/entries?path=...`
- `POST /api/analysis` (plaintext request, compute-only)
- `POST /api/recommendations` (plaintext request, compute-only)

---

## 2) API Contract Map (critical)

### 2.1 New endpoints required (and legacy endpoint removal)

**New**
- `POST /api/auth/register`
- `POST /api/auth/login`

**Legacy to delete/deprecate**
- `POST /api/auth/verify` (password gate)

Assumption (flagged): identity is `username` (matches `UserEntity.username`). If you want `email`, adjust DTO field names and repository methods.

### 2.2 Contract table (frontend → HTTP → backend → DB)

Notes:
- Response `salt`, `titleCiphertext`, etc are base64 strings to keep JSON + TypeScript simple.
- The backend should issue JWTs containing `sub` (username) and a `uid` claim (user UUID) so the controllers/services can reliably scope DB queries.

| Frontend action | Method + path | Request JSON fields (exact) | Response JSON fields (exact) | Backend controller method | Backend service method | DB touched |
|---|---|---|---|---|---|---|
| Register | `POST /api/auth/register` | `{ "username": string, "password": string }` | `{ "accessToken": string, "tokenType": "Bearer", "expiresIn": number, "user": { "id": string, "username": string }, "e2ee": { "kdf": "PBKDF2-SHA256", "salt": string, "iterations": number } }` | `AuthController#register(...)` (new) | `AuthService#register(...)` (new) | `users` |
| Login | `POST /api/auth/login` | `{ "username": string, "password": string }` | same as register response | `AuthController#login(...)` (new) | `AuthService#login(...)` (new) | `users` |
| List entries (home) | `GET /api/entries` | header: `Authorization` | `[{ "storagePath": string, "createdAt": string|null, "updatedAt": string|null, "titleCiphertext": string, "titleIv": string, "algo": string, "version": number }]` | `EntriesController#listEntries(...)` (changed) | `EntryService#list(userId)` (changed) | `diary_entries` |
| Load entry | `GET /api/entries/by-path?path=...` | header: `Authorization` | `{ "storagePath": string, "createdAt": string|null, "updatedAt": string|null, "titleCiphertext": string, "titleIv": string, "bodyCiphertext": string, "bodyIv": string, "algo": string, "version": number }` | `EntriesController#getEntryByPath(...)` (changed) | `EntryService#load(userId, path)` (changed) | `diary_entries` |
| Create entry | `POST /api/entries` | `{ "storagePath": null, "createdAt": string|null, "titleCiphertext": string, "titleIv": string, "bodyCiphertext": string, "bodyIv": string, "algo": string, "version": number }` | same as load entry response | `EntriesController#createEntry(...)` (changed) | `EntryService#save(userId, ...)` (changed) | `diary_entries` |
| Update entry | `PUT /api/entries` | same as create, but `storagePath` must be string | same as load entry response | `EntriesController#updateEntry(...)` (changed) | `EntryService#save(userId, ...)` (changed) | `diary_entries` |
| Delete entry | `DELETE /api/entries?path=...` | header: `Authorization` | `{ "deleted": boolean, "storagePath": string }` | `EntriesController#deleteEntry(...)` (changed) | `EntryService#delete(userId, path)` (changed) | `diary_entries` |
| Analyze keywords | `POST /api/analysis` | `{ "text": string }` (plaintext) + header | `{ "keywords": string[] }` | `AnalysisController#analyze(...)` (security-only change) | `AnalysisService#analyze(text)` | none |
| Get recommendations | `POST /api/recommendations` | `{ "text": string }` (plaintext) + header | `{ "keywords": string[], "songs": [...], "movies": [...] }` | `RecommendationsController#recommend(...)` (security-only change) | `RecommendationService#recommend(text)` | none |

---

## 3) Database + Flyway migrations (critical)

### 3.1 Users table (BCrypt + E2EE params)

Current: `users.password` is plaintext (`V1__init.sql`).

Target (minimum viable columns):
- `password_hash TEXT NOT NULL` (BCrypt)
- `e2ee_kdf TEXT NOT NULL` (fixed value in MVP: `PBKDF2-SHA256`)
- `e2ee_salt BYTEA NOT NULL` (random bytes, not secret)
- `e2ee_iterations INT NOT NULL` (e.g. 310000; choose and document)

Flyway:
- Add `backend/src/main/resources/db/migration/V2__auth_users.sql`
- Migration should:
  - rename `password` → `password_hash`
  - add `e2ee_kdf`, `e2ee_salt`, `e2ee_iterations`

### 3.2 Diary entries table (ciphertext-only storage)

Current: `diary_entries.title` and `diary_entries.text` are plaintext.

Target (ciphertext only; two-field split for list UX):
- `title_ciphertext BYTEA NOT NULL`
- `title_iv BYTEA NOT NULL`
- `body_ciphertext BYTEA NOT NULL`
- `body_iv BYTEA NOT NULL`
- `algo TEXT NOT NULL` (e.g. `AES-256-GCM`)
- `version INT NOT NULL` (envelope version)

Flyway:
- Add `backend/src/main/resources/db/migration/V3__e2ee_entries.sql`
- Migration should:
  - drop or deprecate plaintext columns (`title`, `text`) and stop using them in code
  - add ciphertext columns above
  - update uniqueness/indexing (next section)

### 3.3 Indexing strategy (user scoping + listing)

User scoping requirement: every query must include `user_id`.

Recommended constraints/indexes:
- Replace global uniqueness on storage path with per-user uniqueness:
  - `UNIQUE (user_id, storage_path)`
- Add an index for listing:
  - `INDEX diary_entries_user_updated_at_idx (user_id, updated_at DESC)`

### 3.4 Migration strategy + breaking changes

Known breaking points to call out before coding:
- Entry DTO shapes change (plaintext → ciphertext): frontend and backend must be updated together.
- Existing plaintext entries cannot be migrated into E2EE without client keys. MVP approach is “fresh start” (drop dev DB).

---

## 4) Step-by-step implementation plan (execution-ready)

Each step below must end with:
- updating `auth-e2ee-changes.md` under the same step header
- running the listed verification commands

### PHASE A — Per-user Auth (Spring Security + JWT)

#### Step 0 — Baseline + guardrails

**Goal**
Lock in baseline behavior before introducing Spring Security (it will break tests if not planned).

**Backend changes**
- None.

**Frontend changes**
- None.

**DB migrations**
- None.

**Tests to add/update**
- None (baseline run only).

**Verification**
- Backend: `cd backend; mvn test`
- Frontend: `cd frontend; npm run test`
- Manual: run backend + frontend and confirm current `/` → unlock → home → create entry flow works.

---

#### Step 1 — Add dependencies for Spring Security + JWT (keep build green)

**Goal**
Add the required libraries for BCrypt + JWT issuing/validation without turning protection on yet (or permit all temporarily until Step 4).

**Backend changes**
- Modify `backend/pom.xml`
  - Add `spring-boot-starter-security`
  - Add `spring-security-oauth2-resource-server`
  - Add `spring-security-oauth2-jose`
- Add `backend/src/main/java/com/sentimentscribe/config/JwtProperties.java` (`@ConfigurationProperties`)
  - `secret`, `issuer`, `ttlSeconds`
- Add `backend/src/main/java/com/sentimentscribe/config/SecurityConfig.java`
  - `SecurityFilterChain` placeholder
  - `BCryptPasswordEncoder` bean

**Frontend changes**
- None.

**DB migrations**
- None.

**Tests to add/update**
- None (keep build green; tests may need updates once Security is enforced in Step 4).

**Verification**
- `cd backend; mvn test`

---

#### Step 2 — Flyway V2 + entity updates: BCrypt hashes + E2EE KDF params

**Goal**
Stop storing plaintext passwords and persist per-user E2EE parameters required for key derivation.

**Backend changes**
- Add `backend/src/main/resources/db/migration/V2__auth_users.sql`
  - Rename `users.password` → `users.password_hash`
  - Add:
    - `e2ee_kdf TEXT NOT NULL`
    - `e2ee_salt BYTEA NOT NULL`
    - `e2ee_iterations INT NOT NULL`
- Modify `backend/src/main/java/com/sentimentscribe/persistence/postgres/entity/UserEntity.java`
  - `password` → `passwordHash`
  - Add `e2eeKdf`, `e2eeSalt`, `e2eeIterations`

**Frontend changes**
- None.

**DB migrations**
- `backend/src/main/resources/db/migration/V2__auth_users.sql`

**Tests to add/update**
- Add unit test (recommended): user registration stores BCrypt hash and `matches` works.

**Verification**
- `cd backend; mvn test`
- Manual: start backend with a fresh DB and confirm Flyway applies V1 then V2.

---

#### Step 3 — Add `POST /api/auth/register` and `POST /api/auth/login` (JWT + BCrypt)

**Goal**
Implement real per-user authentication: create accounts, verify passwords via BCrypt, and issue JWT access tokens.

**Backend changes**
- Add DTOs under `backend/src/main/java/com/sentimentscribe/web/dto/`:
  - `RegisterRequest` → `{ username, password }`
  - `LoginRequest` → `{ username, password }`
  - `AuthTokenResponse` → `{ accessToken, tokenType, expiresIn, user, e2ee }`
  - `UserResponse` → `{ id, username }`
  - `E2eeParamsResponse` → `{ kdf, salt, iterations }`
- Add `backend/src/main/java/com/sentimentscribe/service/JwtService.java`
  - Issue tokens with claims: `sub` (username), `uid` (user UUID), `exp`, `iss`
- Modify `backend/src/main/java/com/sentimentscribe/service/AuthService.java`
  - Add `register(username, password)` + `login(username, password)`
- Modify `backend/src/main/java/com/sentimentscribe/web/AuthController.java`
  - Add `@PostMapping("/register")`
  - Add `@PostMapping("/login")`
  - Keep `/verify` temporarily (delete in Step 6)

**Frontend changes**
- None.

**DB migrations**
- None.

**Tests to add/update**
- Add integration tests:
  - register success returns token + e2ee params
  - login success returns token
  - wrong password returns `400` with `ErrorResponse`

**Verification**
- `cd backend; mvn test`
- Manual: register/login and confirm DB stores BCrypt (no plaintext).

---

#### Step 4 — Enable stateless JWT auth (protect endpoints + JSON 401/403)

**Goal**
Turn on real auth: all diary endpoints must require `Authorization: Bearer <token>`.

**Backend changes**
- Modify `backend/src/main/java/com/sentimentscribe/config/SecurityConfig.java`
  - Stateless sessions, CSRF disabled for API
  - JWT validation using HMAC secret from config
  - Permit: `/api/auth/**`, `/api/health`
  - Require auth: all other `/api/**`
  - Ensure CORS works with Spring Security (add a `CorsConfigurationSource` if needed)
  - Ensure 401/403 return JSON `{ "error": "..." }` (match frontend `ApiError`)
- Modify `backend/src/main/resources/application.yml`
  - Add env-var placeholders for JWT config (no real secrets)

**Frontend changes**
- None.

**DB migrations**
- None.

**Tests**
- Update `backend/src/test/java/com/sentimentscribe/web/EntriesApiIntegrationTest.java`
  - Replace `/api/auth/verify` with register/login
  - Add `Authorization` header to entries calls
  - Add negative coverage: missing token → 401

**Verification**
- `cd backend; mvn test`
- Manual: entries endpoints reject unauthenticated requests.

---

#### Step 5 — Enforce per-user authorization on diary endpoints (no cross-user access)

**Goal**
Ensure entries are scoped to the authenticated user and cannot be accessed by guessing `storagePath`.

**Backend changes**
- DB constraint/index:
  - Add Flyway migration to enforce:
    - `UNIQUE (user_id, storage_path)`
    - index `(user_id, updated_at DESC)`
- Repository:
  - Modify `backend/src/main/java/com/sentimentscribe/persistence/postgres/repo/DiaryEntryJpaRepository.java`
    - Add `findByUser_IdAndStoragePath(...)` and `findAllByUser_Id(...)`
- Adapter:
  - Modify `backend/src/main/java/com/sentimentscribe/persistence/postgres/PostgresDiaryEntryRepositoryAdapter.java`
    - Remove `DEFAULT_USERNAME`
    - Require `userId` in every read/write/delete/list
- Controller/service:
  - Modify `backend/src/main/java/com/sentimentscribe/web/EntriesController.java` to read JWT `uid` claim
  - Modify `backend/src/main/java/com/sentimentscribe/service/EntryService.java` signatures to accept `userId`
- Usecase ports:
  - Update entry usecase interfaces to accept `userId` so scoping can't be skipped accidentally.

**Frontend changes**
- None.

**DB migrations**
- Add a Flyway migration that enforces `UNIQUE (user_id, storage_path)` and adds the `(user_id, updated_at DESC)` index.

**Tests**
- Integration: create user A + user B; entry created under A must not be readable/deletable by B.

**Verification**
- `cd backend; mvn test`

---

#### Step 6 — Delete the legacy password gate (`/api/auth/verify` + default-user logic)

**Goal**
Remove the old insecure flow so the repo matches the resume bullet honestly.

**Backend changes**
- Delete/deprecate:
  - `POST /api/auth/verify` from `backend/src/main/java/com/sentimentscribe/web/AuthController.java`
  - `backend/src/main/java/com/sentimentscribe/usecase/verify_password/**`
  - `backend/src/main/java/com/sentimentscribe/persistence/postgres/PostgresVerifyPasswordDataAccessObject.java`
  - Remove the bean from `backend/src/main/java/com/sentimentscribe/config/AppConfig.java`

**Frontend changes**
- None (frontend cleanup happens after Step 7).

**DB migrations**
- None.

**Tests to add/update**
- None (run existing tests).

**Verification**
- `cd backend; mvn test`

---

### PHASE B — Frontend Auth Flow

#### Step 7 - Replace VerifyPasswordPage with Login/Register + token attachment

**Goal**
Make the React app authenticate via JWT and attach the access token to every API request.

**Backend changes**
- None (backend auth endpoints + JWT protection are implemented in Steps 3–5).

**Frontend changes**
- Routing: `frontend/src/routes.tsx`
  - Replace index route with `LoginPage`
  - Add `/register`
  - Replace `RequireUnlocked` with `RequireAuth`
- Pages:
  - Add `frontend/src/pages/LoginPage.tsx`
  - Add `frontend/src/pages/RegisterPage.tsx`
- Auth state:
  - Modify `frontend/src/state/auth.tsx` to store `{ accessToken, user, e2eeParams }` (MVP: `sessionStorage`)
- HTTP client:
  - Modify `frontend/src/api/http.ts` to attach `Authorization: Bearer <token>`
  - Add minimal 401 handler: clear token and navigate to login
- API/types:
  - Modify `frontend/src/api/auth.ts` and `frontend/src/api/types.ts` to match new auth endpoints and DTOs

**DB migrations**
- None.

**Tests to add/update**
- Update/add frontend tests for auth API calls + route guards.

**Verification**
- Frontend: `cd frontend; npm run test`
- Manual: register/login works and home page loads entries with a token.

---

### PHASE C — E2EE Diary Storage

#### Step 8 — Flyway V3 + DTO/entity updates: ciphertext-only diary entries

**Goal**
Backend stores ciphertext only (no plaintext title/text in DB).

**Backend changes**
- Add `backend/src/main/resources/db/migration/V3__e2ee_entries.sql`
  - Add ciphertext columns and drop/deprecate plaintext columns
- Modify `backend/src/main/java/com/sentimentscribe/persistence/postgres/entity/DiaryEntryEntity.java`
  - Replace plaintext `title/text` with ciphertext + iv + algo + version fields
- Modify entry DTOs in `backend/src/main/java/com/sentimentscribe/web/dto/`
  - `EntryRequest`, `EntryResponse`, `EntrySummaryResponse` → ciphertext shapes (section 2.2)
- Modify `backend/src/main/java/com/sentimentscribe/web/EntriesController.java` mapping
- Modify `backend/src/main/java/com/sentimentscribe/persistence/postgres/PostgresDiaryEntryRepositoryAdapter.java` persistence mapping
- Disable server-side keyword extraction for stored entries (cannot work without plaintext)

**Frontend changes**
- Update `frontend/src/api/types.ts` + `frontend/src/api/entries.ts` to match ciphertext DTOs (UI wiring comes later).

**DB migrations**
- `backend/src/main/resources/db/migration/V3__e2ee_entries.sql`

**Tests to add/update**
- Update backend integration tests to use ciphertext DTO shapes (no plaintext assumptions).
- Update/add a negative test that confirms no plaintext fields are returned by entry endpoints.

**Verification**
- Backend: `cd backend; mvn test`
- Manual: create/load/list entries with dummy base64 ciphertext (proves persistence wiring).

---

#### Step 9 — Keep analysis/recommendations working (plaintext on demand; no persistence)

**Goal**
Preserve NLP + recommendations by sending plaintext only when requested, even though stored entries are encrypted.

**Backend changes**
- Keep `POST /api/analysis` and `POST /api/recommendations` compute-only (no DB writes).
- Ensure they are JWT-protected and do not log request bodies.

**Frontend changes**
- Keep sending plaintext `{ text }` from the editor (after decryption in Step 12).

**DB migrations**
- None.

**Tests to add/update**
- Backend: add an integration test that `/api/analysis` and `/api/recommendations` return `401` without a token and `200` with a token.

**Verification**
- Manual: with token, keywords + recommendations still work.

---

### PHASE D — Key Management UX (Minimum Viable)

#### Step 10 — Add Web Crypto utilities (PBKDF2 + AES-GCM)

**Goal**
Implement client-side encryption for the ciphertext envelope stored on the backend.

**Backend changes**
- None.

**Frontend changes**
- Add:
  - `frontend/src/crypto/base64.ts`
  - `frontend/src/crypto/kdf.ts`
  - `frontend/src/crypto/aesGcm.ts`
  - `frontend/src/crypto/envelope.ts`

**DB migrations**
- None.

**Tests to add/update**
- Add crypto round-trip tests (encrypt then decrypt returns original).

**Verification**
- `cd frontend; npm run test`

---

#### Step 11 — Add “Unlock” flow (passphrase → key in memory only)

**Goal**
After login (JWT), require a second step to derive the E2EE key locally.

**Backend changes**
- None (auth endpoints already return `e2ee` params from Step 3).

**Frontend changes**
- Add `frontend/src/state/e2ee.tsx` (new): stores `CryptoKey` in memory only
- Add `frontend/src/pages/UnlockPage.tsx`
- Update `frontend/src/routes.tsx` to require:
  - auth for protected pages
  - unlock for pages that decrypt data

**Edge cases to implement**
- Refresh loses key → requires unlock again.
- Logout clears token + key.
- Multi-tab: each tab unlocks separately (MVP).

**DB migrations**
- None.

**Tests to add/update**
- Add route-guard tests: authenticated-but-locked users are redirected to unlock.
- Add unit tests for `e2ee.unlock(passphrase)` using mocked salts/iterations.

**Verification**
- Manual: refresh → still authenticated (token), but must unlock again to view titles/content.

---

#### Step 12 — Wire E2EE into diary CRUD (encrypt before save, decrypt after load)

**Goal**
End-to-end: plaintext only in browser memory; backend stores ciphertext only.

**Backend changes**
- None (ciphertext-only storage is implemented in Step 8).

**Frontend changes**
- Home list: decrypt `titleCiphertext` before rendering (`frontend/src/pages/HomeMenuPage.tsx`)
- Entry page: decrypt on load, encrypt on save (`frontend/src/pages/DiaryEntryPage.tsx`)
- Types/API: ensure `frontend/src/api/types.ts` + `frontend/src/api/entries.ts` match section 2.2

**DB migrations**
- None.

**Tests to add/update**
- Add frontend tests that mock ciphertext DTOs and assert decrypted rendering for home + entry.

**Verification**
- Manual end-to-end:
  - login → unlock → create entry → reload → unlock → open entry
  - confirm DB has ciphertext fields populated and no plaintext columns used.

---

## 5) Security & privacy notes

**BCrypt (hard requirement)**
- Store passwords only as BCrypt hashes in `users.password_hash` (never plaintext).
- Never log raw passwords or password hashes.

**JWT (hard requirement)**
- Stateless access tokens only (no refresh token needed for MVP).
- Use a strong HMAC secret from env vars (do not commit it).
- JWT must include expiration (`exp`) and backend must enforce it.
- Prefer including `uid` claim so entry endpoints can reliably scope to a user UUID without extra DB lookups per request.

**Token storage (frontend tradeoffs)**
- MVP recommendation: store access token in `sessionStorage` to survive refresh but not browser restart.
- Risk: any XSS can read `sessionStorage`. Mitigations (out of MVP): strong CSP, Trusted Types, avoid `dangerouslySetInnerHTML`, sanitize external content.

**E2EE constraints (hard requirement)**
- Backend stores ciphertext only and cannot decrypt stored entries.
- Passphrase/key never sent to backend.
- Derived `CryptoKey` stays in memory only; refresh requires unlock again.

**Plaintext compute endpoints**
- `/api/analysis` + `/api/recommendations` will receive plaintext; keep them JWT-protected and do not persist or log request bodies.

Not implemented yet (explicitly out of MVP scope)
- Token refresh/rotation/revocation
- Passphrase recovery / key escrow
- Multi-device key sync and encrypted conflict resolution

---

## 6) Testing plan

### Backend (JUnit + Testcontainers)

**Unit tests (add where they fit your patterns)**
- AuthService:
  - register hashes passwords (BCrypt) and refuses duplicate usernames
  - login validates password via BCrypt
- JwtService:
  - issued JWT contains `sub`, `uid`, `exp` and is verifiable by configured decoder

**Integration tests (mandatory updates)**
- Update `backend/src/test/java/com/sentimentscribe/web/EntriesApiIntegrationTest.java`
  - use `/api/auth/register` or `/api/auth/login` instead of `/api/auth/verify`
  - attach `Authorization` headers for entry requests
  - add negative coverage: no token → 401 JSON `{ error }`
- Add a new integration test class (recommended) for isolation:
  - create 2 users and prove user B cannot load/delete user A’s entry by `storagePath`

**Commands**
- `cd backend; mvn test`

### Frontend (Vitest)

**Unit/component tests**
- API auth module tests: correct URL/method/body for register/login
- Route guard tests: unauthenticated redirects to `/`
- Crypto tests: encrypt → decrypt round-trip
- UI tests (minimal): mocked ciphertext API response renders decrypted title/text after unlock

**Commands**
- `cd frontend; npm run test`

### Manual smoke test checklist (end-to-end)

- Register → login → token stored
- Unlock → key in memory only
- Create entry → DB stores ciphertext only
- Refresh → still authenticated but must unlock again
- Cross-user isolation → user B cannot load/delete user A entry
- Analysis/recommendations still work by sending plaintext on demand

---

## 7) Rollout order + risk control

**Recommended order**
1) Auth endpoints + BCrypt (Step 1 → Step 3)
2) JWT protection (Step 4)
3) Per-user scoping in persistence + queries (Step 5) — this is the key “no cross-user access” guarantee
4) Remove legacy password gate (Step 6)
5) Frontend login/register + token attachment (Step 7)
6) Ciphertext-only storage + DTO change (Step 8)
7) Client crypto + unlock + encrypt/decrypt wiring (Step 10 → Step 12)

**Why**
- Per-user scoping must happen before E2EE so you don’t accidentally create “encrypted but globally addressable” entries.
- E2EE is a multi-layer breaking change; it’s much easier once auth and scoping are stable.

**Breaking-change management (dev)**
- Expect a window where backend and frontend are temporarily incompatible (DTO changes). Keep changes on a single branch or merge in lockstep per step.
