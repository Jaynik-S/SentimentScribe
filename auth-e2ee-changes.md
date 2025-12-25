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

## Step 2 — Flyway V2 + entity updates: BCrypt hashes + E2EE KDF params

- Files changed:
- Summary:
- Backend notes:
- Frontend notes:
- DB notes:
- Verification:

---

## Step 3 — Add `POST /api/auth/register` and `POST /api/auth/login` (JWT + BCrypt)

- Files changed:
- Summary:
- Backend notes:
- Frontend notes:
- DB notes:
- Verification:

---

## Step 4 — Enable stateless JWT auth (protect endpoints + JSON 401/403)

- Files changed:
- Summary:
- Backend notes:
- Frontend notes:
- DB notes:
- Verification:

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
