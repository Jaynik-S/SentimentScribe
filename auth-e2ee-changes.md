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
- Verification: `cd backend; mvn test` (pass). Frontend tests not run.

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

## Step 5 - Enforce per-user authorization on diary endpoints (no cross-user access)

- Files changed: backend/src/main/resources/db/migration/V4__entries_user_scope.sql; backend/src/main/java/com/sentimentscribe/persistence/postgres/repo/DiaryEntryJpaRepository.java; backend/src/main/java/com/sentimentscribe/persistence/postgres/PostgresDiaryEntryRepositoryAdapter.java; backend/src/main/java/com/sentimentscribe/service/EntryService.java; backend/src/main/java/com/sentimentscribe/web/EntriesController.java; backend/src/main/java/com/sentimentscribe/usecase/save_entry/SaveEntryUserDataAccessInterface.java; backend/src/main/java/com/sentimentscribe/usecase/load_entry/LoadEntryUserDataAccessInterface.java; backend/src/main/java/com/sentimentscribe/usecase/delete_entry/DeleteEntryUserDataAccessInterface.java; backend/src/main/java/com/sentimentscribe/usecase/verify_password/RenderEntriesUserDataInterface.java; backend/src/main/java/com/sentimentscribe/usecase/verify_password/VerifyPasswordUserDataAccessInterface.java; backend/src/main/java/com/sentimentscribe/usecase/verify_password/VerifyPasswordInteractor.java; backend/src/main/java/com/sentimentscribe/usecase/save_entry/SaveEntryInputData.java; backend/src/main/java/com/sentimentscribe/usecase/load_entry/LoadEntryInputData.java; backend/src/main/java/com/sentimentscribe/usecase/delete_entry/DeleteEntryInputData.java; backend/src/main/java/com/sentimentscribe/usecase/save_entry/SaveEntryInteractor.java; backend/src/main/java/com/sentimentscribe/usecase/load_entry/LoadEntryInteractor.java; backend/src/main/java/com/sentimentscribe/usecase/delete_entry/DeleteEntryInteractor.java; backend/src/main/java/com/sentimentscribe/persistence/postgres/PostgresVerifyPasswordDataAccessObject.java; backend/src/test/java/com/sentimentscribe/usecase/save_entry/SaveEntryInteractorTest.java; backend/src/test/java/com/sentimentscribe/usecase/load_entry/LoadEntryInteractorTest.java; backend/src/test/java/com/sentimentscribe/usecase/delete_entry/DeleteEntryInteractorTest.java; backend/src/test/java/com/sentimentscribe/usecase/verify_password/VerifyPasswordInteractorTest.java; auth-e2ee-changes.md
- Summary: Added per-user DB constraints/indexing and enforced user scoping across entry CRUD/list flows (controller → service → usecase → repository).
- Backend notes: Entry endpoints now read JWT `uid` and pass it through usecase input data to repository calls. Added user-scoped repository methods and safety checks. Legacy verify-password flow now resolves the default user ID to scope entry listing.
- Frontend notes: None.
- DB notes: Added V4 migration to replace global storage_path uniqueness with `(user_id, storage_path)` and added `(user_id, updated_at DESC)` index.
- Verification: `cd backend; mvn test`

---

## Step 6 - Delete the legacy password gate (`/api/auth/verify` + default-user logic)

- Files changed: backend/src/main/java/com/sentimentscribe/web/AuthController.java; backend/src/main/java/com/sentimentscribe/service/AuthService.java; backend/src/main/java/com/sentimentscribe/data/DiaryEntryRepository.java; backend/src/main/java/com/sentimentscribe/config/AppConfig.java; backend/src/main/java/com/sentimentscribe/usecase/save_entry/SaveEntryUserDataAccessInterface.java; backend/src/main/java/com/sentimentscribe/usecase/load_entry/LoadEntryUserDataAccessInterface.java; backend/src/main/java/com/sentimentscribe/usecase/delete_entry/DeleteEntryUserDataAccessInterface.java; backend/src/main/java/com/sentimentscribe/web/dto/AuthRequest.java; backend/src/main/java/com/sentimentscribe/web/dto/AuthResponse.java; backend/src/main/java/com/sentimentscribe/persistence/postgres/PostgresVerifyPasswordDataAccessObject.java; backend/src/main/java/com/sentimentscribe/usecase/verify_password/RenderEntriesUserDataInterface.java; backend/src/main/java/com/sentimentscribe/usecase/verify_password/VerifyPasswordInputBoundary.java; backend/src/main/java/com/sentimentscribe/usecase/verify_password/VerifyPasswordInputData.java; backend/src/main/java/com/sentimentscribe/usecase/verify_password/VerifyPasswordOutputBoundary.java; backend/src/main/java/com/sentimentscribe/usecase/verify_password/VerifyPasswordOutputData.java; backend/src/main/java/com/sentimentscribe/usecase/verify_password/VerifyPasswordInteractor.java; backend/src/main/java/com/sentimentscribe/usecase/verify_password/VerifyPasswordUserDataAccessInterface.java; backend/src/test/java/com/sentimentscribe/usecase/verify_password/VerifyPasswordInteractorTest.java; auth-e2ee-changes.md
- Summary: Removed legacy password-gate endpoint and verify-password usecase/data access code, leaving only register/login for auth.
- Backend notes: `/api/auth/verify` and related DTOs/usecase classes were deleted; AuthService now only supports register/login. DiaryEntryRepository now declares list method directly after removing verify-password interfaces.
- Frontend notes: None (frontend cleanup is scheduled for Step 7).
- DB notes: None.
- Verification: `cd backend; mvn test`

---

## Step 7 - Replace VerifyPasswordPage with Login/Register + token attachment

- Files changed: frontend/src/api/auth.ts; frontend/src/api/types.ts; frontend/src/api/http.ts; frontend/src/api/debug.ts; frontend/src/state/auth.tsx; frontend/src/routes.tsx; frontend/src/pages/LoginPage.tsx; frontend/src/pages/RegisterPage.tsx; frontend/src/pages/VerifyPasswordPage.tsx (deleted); frontend/src/pages/__tests__/LoginPage.test.tsx; frontend/src/pages/__tests__/RegisterPage.test.tsx; frontend/src/pages/__tests__/VerifyPasswordPage.test.tsx (deleted); frontend/src/api/__tests__/auth.test.ts; frontend/src/api/__tests__/http.test.ts; frontend/src/routes.test.tsx; frontend/src/test/setup.ts; auth-e2ee-changes.md
- Summary: Replaced the password gate with login/register screens, added JWT session storage in `sessionStorage`, and attached bearer tokens to API calls with a 401 redirect.
- Backend notes: None.
- Frontend notes: Added Login/Register pages and `/register` route, swapped `RequireUnlocked` for `RequireAuth`, and revamped auth state to store `{ accessToken, user, e2eeParams }` in `sessionStorage["sentimentscribe.auth"]`. HTTP client now injects `Authorization: Bearer <token>` and clears auth on 401. Updated auth/API/route guard tests and debug helpers.
- DB notes: None.
- Verification: `cd frontend; npm run test` (fails at `src/pages/__tests__/DiaryEntryPage.test.tsx` "requests recommendations and navigates": missing `[data-testid="location"]`, same failure as Step 0).

---

## Step 8 - Flyway V3 + DTO/entity updates: ciphertext-only diary entries

- Files changed: backend/src/main/resources/db/migration/V3__e2ee_entries.sql; backend/src/main/java/com/sentimentscribe/domain/DiaryEntry.java; backend/src/main/java/com/sentimentscribe/persistence/postgres/entity/DiaryEntryEntity.java; backend/src/main/java/com/sentimentscribe/persistence/postgres/PostgresDiaryEntryRepositoryAdapter.java; backend/src/main/java/com/sentimentscribe/service/EntryCommand.java; backend/src/main/java/com/sentimentscribe/service/EntryService.java; backend/src/main/java/com/sentimentscribe/usecase/save_entry/SaveEntryInputData.java; backend/src/main/java/com/sentimentscribe/usecase/save_entry/SaveEntryOutputData.java; backend/src/main/java/com/sentimentscribe/usecase/save_entry/SaveEntryInteractor.java; backend/src/main/java/com/sentimentscribe/usecase/load_entry/LoadEntryOutputData.java; backend/src/main/java/com/sentimentscribe/usecase/load_entry/LoadEntryInteractor.java; backend/src/main/java/com/sentimentscribe/web/EntriesController.java; backend/src/main/java/com/sentimentscribe/web/dto/EntryRequest.java; backend/src/main/java/com/sentimentscribe/web/dto/EntryResponse.java; backend/src/main/java/com/sentimentscribe/web/dto/EntrySummaryResponse.java; backend/src/main/java/com/sentimentscribe/config/AppConfig.java; backend/src/test/java/com/sentimentscribe/web/EntriesApiIntegrationTest.java; backend/src/test/java/com/sentimentscribe/usecase/save_entry/SaveEntryInteractorTest.java; backend/src/test/java/com/sentimentscribe/usecase/load_entry/LoadEntryInteractorTest.java; backend/src/test/java/com/sentimentscribe/domain/DiaryEntryTest.java; frontend/src/api/types.ts; frontend/src/api/__tests__/entries.test.ts; frontend/src/pages/DiaryEntryPage.tsx; frontend/src/components/EntriesTable.tsx; frontend/src/components/DeleteEntryModal.tsx; frontend/src/pages/__tests__/DiaryEntryPage.test.tsx; frontend/src/pages/__tests__/HomeMenuPage.test.tsx; auth-e2ee-changes.md
- Summary: Migrated diary entries to ciphertext-only storage and updated backend/front-end DTOs and tests to use ciphertext fields, removing server-side keyword extraction for stored entries.
- Backend notes: Added V3 migration for ciphertext columns and refactored entry domain/entity/usecase/controller mappings to use `{title/body}Ciphertext`, IVs, algo, and version. Save/load validators now enforce ciphertext fields, and list responses include ciphertext summaries only. Added an integration test asserting plaintext fields are omitted.
- Frontend notes: Entry API types now use ciphertext fields; diary UI continues using plaintext as ciphertext placeholders with default IV/algo/version, and entries table/delete modal display the ciphertext title.
- DB notes: V3 migration adds ciphertext/IV/algo/version columns and drops plaintext title/text columns.
- Verification: `cd backend; mvn test`

---

## Step 9 - Keep analysis/recommendations working (plaintext on demand; no persistence)

- Files changed: backend/src/test/java/com/sentimentscribe/web/AnalysisRecommendationsApiIntegrationTest.java; auth-e2ee-changes.md
- Summary: Added JWT coverage tests for `/api/analysis` and `/api/recommendations` to confirm 401 without tokens and 200 with tokens.
- Backend notes: Integration test uses mocked AnalysisService/RecommendationService responses to avoid external API calls while still exercising the security filter.
- Frontend notes: None.
- DB notes: None.
- Verification: `cd backend; mvn test` (pass; shows Spring warning about deprecated `@MockBean`).

---

## Step 10 - Add Web Crypto utilities (PBKDF2 + AES-GCM)

- Files changed: frontend/src/crypto/base64.ts; frontend/src/crypto/kdf.ts; frontend/src/crypto/aesGcm.ts; frontend/src/crypto/envelope.ts; frontend/src/crypto/__tests__/crypto.test.ts; frontend/src/test/setup.ts; auth-e2ee-changes.md
- Summary: Added Web Crypto helpers for base64, PBKDF2 key derivation, AES-GCM encryption, and encrypted envelopes, plus round-trip tests.
- Backend notes: None.
- Frontend notes: New crypto utilities live under `frontend/src/crypto` with a test harness that uses Web Crypto in Vitest.
- DB notes: None.
- Verification: `cd frontend; npm run test` (fails at `src/pages/__tests__/DiaryEntryPage.test.tsx` "requests recommendations and navigates": missing `[data-testid="location"]`, same failure as earlier).

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
