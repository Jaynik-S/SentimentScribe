# Auth + E2EE Change Log (Template)

> Rule: after each step in `auth-e2ee-plan.md`, record implementation notes **only** in this file (no per-step files).

---

## Step 0 - Baseline + guardrails

- Files changed: auth-e2ee-changes.md
- Summary: Ran baseline backend/frontend tests; backend passed, frontend has one failing test.
- Backend notes: `mvn test` passed (Testcontainers spun up Postgres; no code changes).
- Frontend notes: `npm run test` failed at `src/pages/__tests__/DiaryEntryPage.test.tsx` (missing `[data-testid="location"]` in "requests recommendations and navigates").
- DB notes: None.
- Verification: `cd backend; mvn test` (pass). `cd frontend; npm run test` (fail; see test above). Manual flow not run.

---

## Step 1 - Add dependencies for Spring Security + JWT (keep build green)

- Files changed: backend/pom.xml; backend/src/main/java/com/sentimentscribe/config/JwtProperties.java; backend/src/main/java/com/sentimentscribe/config/SecurityConfig.java; auth-e2ee-changes.md
- Summary: Added Spring Security/JWT dependencies plus placeholder security config and JWT properties while permitting all requests.
- Backend notes: `SecurityConfig` disables CSRF and permits all requests with a BCrypt encoder; implements `WebMvcConfigurer` so MVC slice tests pick up the config. Added `JwtProperties` for upcoming JWT config.
- Frontend notes: None.
- DB notes: None.
- Verification: `cd backend; mvn test`

---

## Step 2 - Flyway V2 + entity updates: BCrypt hashes + E2EE KDF params

- Files changed: backend/src/main/resources/db/migration/V2__auth_users.sql; backend/src/main/java/com/sentimentscribe/persistence/postgres/entity/UserEntity.java; backend/src/main/java/com/sentimentscribe/persistence/postgres/PostgresVerifyPasswordDataAccessObject.java; backend/src/main/java/com/sentimentscribe/config/AppConfig.java; auth-e2ee-changes.md
- Summary: Added Flyway V2 for auth fields, updated UserEntity for password hash + E2EE params, and seeded defaults in legacy password flow.
- Backend notes: `UserEntity` now maps `password_hash` and E2EE fields. Legacy verify-password DAO now hashes with BCrypt and populates E2EE defaults (PBKDF2-SHA256, 310000, random 16-byte salt).
- Frontend notes: None.
- DB notes: Added V2 migration renaming `password` to `password_hash` and adding `e2ee_kdf`, `e2ee_salt`, `e2ee_iterations` (all NOT NULL).
- Verification: `cd backend; mvn test`. Manual Flyway run not performed beyond tests.

---

## Step 3 - Add `POST /api/auth/register` and `POST /api/auth/login` (JWT + BCrypt)

- Files changed: backend/src/main/java/com/sentimentscribe/service/AuthService.java; backend/src/main/java/com/sentimentscribe/service/JwtService.java; backend/src/main/java/com/sentimentscribe/web/AuthController.java; backend/src/main/java/com/sentimentscribe/web/dto/RegisterRequest.java; backend/src/main/java/com/sentimentscribe/web/dto/LoginRequest.java; backend/src/main/java/com/sentimentscribe/web/dto/AuthTokenResponse.java; backend/src/main/java/com/sentimentscribe/web/dto/UserResponse.java; backend/src/main/java/com/sentimentscribe/web/dto/E2eeParamsResponse.java; backend/src/test/java/com/sentimentscribe/web/AuthApiIntegrationTest.java; auth-e2ee-changes.md
- Summary: Added register/login endpoints with BCrypt-backed user creation and JWT issuance, plus integration tests for auth flows.
- Backend notes: AuthService now creates users with E2EE params, validates login via BCrypt, and issues JWTs (sub/uid/exp/iss). New DTOs match the planned response contract. `/api/auth/verify` remains unchanged.
- Frontend notes: None.
- DB notes: None.
- Verification: `cd backend; mvn test`

---

## Step 4 - Enable stateless JWT auth (protect endpoints + JSON 401/403)

- Files changed: backend/src/main/java/com/sentimentscribe/config/SecurityConfig.java; backend/src/main/resources/application.yml; backend/src/test/java/com/sentimentscribe/web/EntriesApiIntegrationTest.java; auth-e2ee-changes.md
- Summary: Enabled stateless JWT auth for `/api/**` with JSON 401/403 handling, configured JWT env placeholders, and updated entries integration tests to use register/login + bearer tokens.
- Backend notes: Security config now permits `/api/auth/**` + `/api/health`, requires JWT for other `/api/**`, and returns `{ "error": "Unauthorized" }` or `{ "error": "Forbidden" }`. Added JwtDecoder for HMAC secrets and enabled CORS + stateless sessions.
- Frontend notes: None.
- DB notes: None.
- Verification: `cd backend; mvn test`

---

## Step 5 — Enforce per-user authorization on diary endpoints (no cross-user access)

- Files changed:
- Summary:
- Backend notes:
- Frontend notes:
- DB notes:
- Verification:

---

## Step 6 — Delete the legacy password gate (`/api/auth/verify` + default-user logic)

- Files changed:
- Summary:
- Backend notes:
- Frontend notes:
- DB notes:
- Verification:

---

## Step 7 — Replace VerifyPasswordPage with Login/Register + token attachment

- Files changed:
- Summary:
- Backend notes:
- Frontend notes:
- DB notes:
- Verification:

---

## Step 8 — Flyway V3 + DTO/entity updates: ciphertext-only diary entries

- Files changed:
- Summary:
- Backend notes:
- Frontend notes:
- DB notes:
- Verification:

---

## Step 9 — Keep analysis/recommendations working (plaintext on demand; no persistence)

- Files changed:
- Summary:
- Backend notes:
- Frontend notes:
- DB notes:
- Verification:

---

## Step 10 — Add Web Crypto utilities (PBKDF2 + AES-GCM)

- Files changed:
- Summary:
- Backend notes:
- Frontend notes:
- DB notes:
- Verification:

---

## Step 11 — Add “Unlock” flow (passphrase → key in memory only)

- Files changed:
- Summary:
- Backend notes:
- Frontend notes:
- DB notes:
- Verification:

---

## Step 12 — Wire E2EE into diary CRUD (encrypt before save, decrypt after load)

- Files changed:
- Summary:
- Backend notes:
- Frontend notes:
- DB notes:
- Verification:
