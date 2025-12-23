# Auth + E2EE Change Log (Template)

> Rule: after each step in `auth-e2ee-plan.md`, record implementation notes **only** in this file (no per-step files).

---

## Step 0 — Baseline + guardrails

- Files changed:
- Summary:
- Backend notes:
- Frontend notes:
- DB notes:
- Verification:

---

## Step 1 — Add dependencies for Spring Security + JWT (keep build green)

- Files changed:
- Summary:
- Backend notes:
- Frontend notes:
- DB notes:
- Verification:

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

