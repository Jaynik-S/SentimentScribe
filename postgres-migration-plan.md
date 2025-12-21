# PostgreSQL Migration Plan - Replace File-Based Storage (DiaryDiscovery)

## Table of Contents

- [Intent + Non-Goals](#intent--non-goals)
- [Current State Analysis (Repo-Specific)](#current-state-analysis-repo-specific)
  - [Backend API Surface (as implemented)](#backend-api-surface-as-implemented)
  - [File-Based Diary Entry Storage](#file-based-diary-entry-storage)
  - [Password Verification (No Users Yet)](#password-verification-no-users-yet)
- [Target Data Model (PostgreSQL)](#target-data-model-postgresql)
  - [Schema Summary](#schema-summary)
  - [Transient Computed Fields (Not Stored)](#transient-computed-fields-not-stored)
- [Backend Implementation Approach](#backend-implementation-approach)
- [API Compatibility Map (Critical)](#api-compatibility-map-critical)
- [Step 0 - Freeze Baseline + Scope Decisions](#step-0--freeze-baseline--scope-decisions)
- [Step 1 - Add PostgreSQL + Flyway + Local Docker](#step-1--add-postgresql--flyway--local-docker)
- [Step 2 - Create Flyway Migrations (Schema)](#step-2--create-flyway-migrations-schema)
- [Step 3 - Add JPA Entities + Spring Data Repositories](#step-3--add-jpa-entities--spring-data-repositories)
- [Step 4 - Implement Postgres Adapters Behind Existing Interfaces](#step-4--implement-postgres-adapters-behind-existing-interfaces)
- [Step 5 - Cutover + Cleanup (Remove File Storage)](#step-5--cutover--cleanup-remove-file-storage)
- [Cleanup Policy (What Gets Deleted When)](#cleanup-policy-what-gets-deleted-when)

## Intent + Non-Goals

This plan replaces the backend’s **file-based persistence** with a **PostgreSQL persistence layer** while keeping the existing API behavior stable.

Mandatory clarified scope for this revision:
- No legacy JSON/file backfill: PostgreSQL starts clean and becomes the sole source of truth.
- PostgreSQL stores ONLY:
  - Users: `username`, `password` (plaintext for now), timestamps
  - Diary entries: `title`, `text`, `created_at`, `updated_at`, `user` relationship, and an API identifier (`storagePath`)
- PostgreSQL MUST NOT store: extracted keywords, sentiment/analysis results, song recommendations, movie recommendations.

Non-goals for this migration (explicitly deferred):
- Password hashing/encryption (store plaintext for now).
- Auth tokens/roles/authorization (no JWT, no Spring Security rules yet).
- New end-user features; any required API changes are called out explicitly and designed to be backward-compatible.

## Current State Analysis (Repo-Specific)

### Backend API Surface (as implemented)

Controllers (backend):
- `backend/src/main/java/com/sentimentscribe/web/AuthController.java` - `POST /api/auth/verify`
- `backend/src/main/java/com/sentimentscribe/web/EntriesController.java` - entries CRUD
- `backend/src/main/java/com/sentimentscribe/web/AnalysisController.java` - `POST /api/analysis`
- `backend/src/main/java/com/sentimentscribe/web/RecommendationsController.java` - `POST /api/recommendations`
- `backend/src/main/java/com/sentimentscribe/web/HealthController.java` - `GET /api/health`

Frontend calls (compatibility reference):
- `frontend/src/api/auth.ts` - `POST /api/auth/verify`
- `frontend/src/api/entries.ts` - `GET /api/entries`, `GET /api/entries/by-path`, `POST /api/entries`, `PUT /api/entries`, `DELETE /api/entries?path=...`
- `frontend/src/api/analysis.ts` - `POST /api/analysis`
- `frontend/src/api/recommendations.ts` - `POST /api/recommendations`

### File-Based Diary Entry Storage

Where persistence happens today:
- Repository interface: `backend/src/main/java/com/sentimentscribe/data/DiaryEntryRepository.java`
- File implementation: `backend/src/main/java/com/sentimentscribe/data/DBNoteDataObject.java`
- Wiring: `backend/src/main/java/com/sentimentscribe/config/AppConfig.java` provides `@Bean DBNoteDataObject` and Spring injects it as `DiaryEntryRepository`.

File location + naming:
- Base directory constant: `DBNoteDataObject.DEFAULT_BASE_DIR = Paths.get("src/main/java/com/sentimentscribe/data/diary_entry_database")`
- Each entry is a `*.json` file in that directory.
- New entry filenames are generated as: `"<nextIndex>) <sanitizeTitle(title)>.json"`
  - `nextIndex` is computed by scanning existing filenames and taking `max(prefix) + 1`
  - invalid filename characters are replaced with `_` via `sanitizeTitle`

How entries are identified today (critical for API compatibility):
- The system uses `storagePath` as the stable identifier:
  - list returns `storagePath`
  - load/delete accept `path` query param which is a `storagePath`
  - save/update accept `storagePath` in the request to target the existing entry
- In file storage, `storagePath` is literally a filesystem path string.

Current entry JSON format (as written by `DBNoteDataObject.save()`):
```json
{
  "title": "…",
  "text": "…",
  "keywords": ["…"],
  "created_date": "2025-11-30T10:43:00.135565700",
  "updated_date": "2025-11-30T10:43:00.137568200",
  "storage_path": "src/main/java/com/sentimentscribe/data/diary_entry_database/1) Title.json"
}
```

Scope implication (mandatory for this revision):
- The Postgres system starts with a clean database and **does not import** these files. Any legacy file-based entries can be ignored entirely.

### Password Verification (No Users Yet)

Where password verification happens today:
- Controller: `backend/src/main/java/com/sentimentscribe/web/AuthController.java` (`POST /api/auth/verify`)
- Service: `backend/src/main/java/com/sentimentscribe/service/AuthService.java`
- Use-case: `backend/src/main/java/com/sentimentscribe/usecase/verify_password/VerifyPasswordInteractor.java`
- Data access implementation: `backend/src/main/java/com/sentimentscribe/data/VerifyPasswordDataAccessObject.java`
- Config source: `backend/src/main/resources/application.yml` -> `sentimentscribe.auth.password` (env `SENTIMENTSCRIBE_PASSWORD`)

Current behavior (must be preserved unless explicitly changed):
- If configured password matches input -> status `"Correct Password"` and returns **all entries**.
- If configured password is blank/null -> first verify call sets password **in-memory** and returns `"Created new password."`, then future verifies require that password.
- If mismatch -> status `"Incorrect Password"` and request fails.

Important limitation today:
- There is no user model (no username, no user storage). It is effectively a single “diary password gate”.

## Target Data Model (PostgreSQL)

### Schema Summary

Guiding constraints from the existing API:
- Keep `storagePath` as the externally-visible identifier used by the frontend/controllers.
- Add internal IDs/relationships so security can be added later without major rewrites.

Proposed tables:

1) `users`
- `id` UUID PK
- `username` TEXT UNIQUE NOT NULL
- `password` TEXT NOT NULL (plaintext for now)
- `created_at` TIMESTAMP NOT NULL
- `updated_at` TIMESTAMP NOT NULL

2) `diary_entries`
- `id` UUID PK
- `user_id` UUID NOT NULL FK -> `users(id)`
- `storage_path` TEXT UNIQUE NOT NULL
- `title` TEXT NOT NULL
- `text` TEXT NOT NULL
- `created_at` TIMESTAMP NOT NULL
- `updated_at` TIMESTAMP NOT NULL

Notes on “per user” requirement vs current API:
- The API currently has no username concept. To keep the API stable, create a single default user (e.g., username `"default"`) and associate all entries to that user.
- Future auth can introduce real multi-user selection without changing `diary_entries` structure (already has `user_id`).

### Transient Computed Fields (Not Stored)

Mandatory constraint for this revision:
- Keywords, analysis/sentiment results, and recommendations are transient and computed on demand; they are returned via API responses only and are not persisted in PostgreSQL.

Repo-specific impact:
- Entry DTOs include `keywords`:
  - `backend/src/main/java/com/sentimentscribe/web/dto/EntrySummaryResponse.java`
  - `backend/src/main/java/com/sentimentscribe/web/dto/EntryResponse.java`
- To avoid breaking the frontend, keywords should be computed on demand when returning list/load responses.
  - Compute from `title + "\n\n" + text` using the existing extractor wiring (`NLPKeywordExtractor` / `SaveEntryKeywordExtractor`).
  - If extraction fails, return `[]` (consistent with current save behavior which falls back to empty keywords on extraction exceptions).

## Backend Implementation Approach

Persistence approach:
- Use **Spring Data JPA** + Hibernate.
- Use **Flyway** for schema migrations.

Keep clean architecture boundaries (repo-specific):
- Controllers remain thin and continue calling:
  - `EntryService` (`backend/src/main/java/com/sentimentscribe/service/EntryService.java`)
  - `AuthService` (`backend/src/main/java/com/sentimentscribe/service/AuthService.java`)
- Use-cases continue depending on existing data-access interfaces.
- Replace persistence behind these interfaces/adapters:
  - `DiaryEntryRepository` (currently implemented by `DBNoteDataObject`)
  - `VerifyPasswordUserDataAccessInterface` (currently implemented by `VerifyPasswordDataAccessObject`)

StoragePath strategy:
- Preserve the “storagePath as identifier” contract by storing `diary_entries.storage_path` and querying by it.
- In Postgres mode, treat `storagePath` as an opaque key (recommended: `"db:" + <uuid>`).

Transient keywords strategy (to keep API stable without DB storage):
- On create/update (`POST /api/entries`, `PUT /api/entries`), the response can use keywords computed during the save flow (already computed by `SaveEntryInteractor` when missing).
- On list/load (`GET /api/entries`, `GET /api/entries/by-path`), compute keywords on demand from stored `title` and `text` before building response DTOs.

## API Compatibility Map (Critical)

The goal is “frontend still works” with unchanged paths/methods and compatible payloads.

| Endpoint | Controller method | Service method | Current file implementation | New DB replacement |
|---|---|---|---|---|
| `GET /api/health` | `HealthController.health()` | (none) | (none) | (none) |
| `POST /api/auth/verify` | `AuthController.verifyPassword()` | `AuthService.verifyPassword()` | `VerifyPasswordDataAccessObject.verifyPassword()` + `DBNoteDataObject.getAll()` | `PostgresVerifyPasswordDataAccessObject.verifyPassword()` + `PostgresDiaryEntryRepositoryAdapter.getAll()` (keywords computed on demand) |
| `GET /api/entries` | `EntriesController.listEntries()` | `EntryService.list()` | `DBNoteDataObject.getAll()` | `PostgresDiaryEntryRepositoryAdapter.getAll()` (keywords computed on demand) |
| `GET /api/entries/by-path?path=...` | `EntriesController.getEntryByPath()` | `EntryService.load(path)` | `DBNoteDataObject.getByPath(path)` | `PostgresDiaryEntryRepositoryAdapter.getByPath(path)` (lookup by `storage_path`, keywords computed on demand) |
| `POST /api/entries` | `EntriesController.createEntry()` | `EntryService.save(command)` | `DBNoteDataObject.save(entry)` | `PostgresDiaryEntryRepositoryAdapter.save(entry)` (insert; ignores keywords for persistence) |
| `PUT /api/entries` | `EntriesController.updateEntry()` | `EntryService.save(command)` | `DBNoteDataObject.save(entry)` (overwrite file by `storagePath`) | `PostgresDiaryEntryRepositoryAdapter.save(entry)` (update by `storage_path`; ignores keywords for persistence) |
| `DELETE /api/entries?path=...` | `EntriesController.deleteEntry()` | `EntryService.delete(path)` | `DBNoteDataObject.deleteByPath(path)` | `PostgresDiaryEntryRepositoryAdapter.deleteByPath(path)` (delete by `storage_path`) |
| `POST /api/analysis` | `AnalysisController.analyze()` | `AnalysisService.analyze(text)` | NLP only (no persistence) | unchanged |
| `POST /api/recommendations` | `RecommendationsController.recommend()` | `RecommendationService.recommend(text)` | external API only (no persistence) | unchanged |

## Step 0 - Freeze Baseline + Scope Decisions

### Goal
Lock down what must stay stable during the migration (API + semantics), and document the clarified scope constraints for a clean Postgres start.

### Exact changes
- Document and confirm:
  - `storagePath` remains the external identifier for entries.
  - Password verify status strings remain exactly: `"Correct Password"`, `"Incorrect Password"`, `"Created new password."`
- Confirm and document (mandatory):
  - No JSON/file data migration/backfill.
  - DB stores only users + diary entries (no persisted keywords/analysis/recommendations).
- Decide cutover approach:
  - Use Spring profiles: `file` (current) and `postgres` (new); keep default as `file` until Step 5.
- Decide `storagePath` format in Postgres mode:
  - Recommend `"db:" + <uuid>` for new entries.
- Document the expected behavior change from a clean DB start:
  - After cutover, legacy file-based `storagePath` values will not resolve (because the DB starts empty). This is expected per scope.

### Files touched (planned)
- `postgres-migration-plan.md`
- `postgres-migration-changes.md`
- `README.md` (optional: short note about Postgres profile + clean start)

### Verification checklist
- `cd backend; mvn -q test`
- Manual smoke (existing file mode before any DB changes):
  - `POST http://localhost:8080/api/auth/verify`
  - `GET http://localhost:8080/api/entries`

## Step 1 - Add PostgreSQL + Flyway + Local Docker

### Goal
Introduce Postgres connectivity and migrations infrastructure without changing default runtime behavior (still file-based by default).

### Exact changes
- Add Maven dependencies in `backend/pom.xml`:
  - `spring-boot-starter-data-jpa`
  - `org.postgresql:postgresql`
  - `org.flywaydb:flyway-core`
  - (tests) `org.testcontainers:postgresql` + `org.testcontainers:junit-jupiter` (for DB integration tests)
- Add `backend/src/main/resources/application-postgres.yml` with:
  - `spring.datasource.url`, `spring.datasource.username`, `spring.datasource.password`
  - `spring.jpa.hibernate.ddl-auto=validate`
  - `spring.flyway.enabled=true`
- Add local Postgres container definition:
  - Prefer `docker-compose.yml` at repo root with a `postgres` service (or `docker-compose.postgres.yml` if you want isolation).

### Files touched (planned)
- `backend/pom.xml`
- `backend/src/main/resources/application-postgres.yml`
- `docker-compose.yml` (repo root) or `docker-compose.postgres.yml`
- `README.md` (add “Local Postgres” run notes)

### Verification checklist
- Start DB: `docker compose up -d postgres` (or the compose file name you chose)
- Validate connectivity (one of):
  - `psql ... -c "select 1;"`
  - connect via a DB client GUI
- Backend still passes tests in default (file) mode: `cd backend; mvn -q test`

## Step 2 - Create Flyway Migrations (Schema)

### Goal
Create the initial schema in a repeatable way (clean DB start; no import/backfill).

### Exact changes
- Add Flyway migration under `backend/src/main/resources/db/migration/`:
  - `V1__init.sql` creating `users` and `diary_entries`
- Include constraints/indexes for current API patterns:
  - `diary_entries(storage_path)` UNIQUE (load/delete/update by storagePath)
  - FK `diary_entries.user_id -> users.id`
- Keep schema minimal and aligned with scope (no keyword/recommendation/analysis tables or columns).

### Files touched (planned)
- `backend/src/main/resources/db/migration/V1__init.sql`

### Verification checklist
- Run backend in DB profile to apply migrations:
  - `SPRING_PROFILES_ACTIVE=postgres cd backend; mvn -q spring-boot:run`
- Verify tables exist:
  - `psql ... -c "\\dt"`

## Step 3 - Add JPA Entities + Spring Data Repositories

### Goal
Add DB persistence models while keeping the web/use-case layers unchanged.

### Exact changes
- Add JPA entities (suggested package):
  - `backend/src/main/java/com/sentimentscribe/persistence/postgres/entity/UserEntity.java`
  - `backend/src/main/java/com/sentimentscribe/persistence/postgres/entity/DiaryEntryEntity.java`
- Add Spring Data repositories:
  - `backend/src/main/java/com/sentimentscribe/persistence/postgres/repo/UserJpaRepository.java`
  - `backend/src/main/java/com/sentimentscribe/persistence/postgres/repo/DiaryEntryJpaRepository.java`
- Ensure mapping includes:
  - `DiaryEntryEntity.storagePath` with UNIQUE constraint in DB
  - `DiaryEntryEntity.user` (or `userId`) relation to `UserEntity`
- Do NOT add keyword/recommendation/analysis fields to entities.

### Files touched (planned)
- `backend/src/main/java/com/sentimentscribe/persistence/postgres/entity/UserEntity.java`
- `backend/src/main/java/com/sentimentscribe/persistence/postgres/entity/DiaryEntryEntity.java`
- `backend/src/main/java/com/sentimentscribe/persistence/postgres/repo/UserJpaRepository.java`
- `backend/src/main/java/com/sentimentscribe/persistence/postgres/repo/DiaryEntryJpaRepository.java`

### Verification checklist
- `cd backend; mvn -q test`
- Optional (if you add repository tests): `cd backend; mvn -q test -Dtest=*Repository*Test`

## Step 4 - Implement Postgres Adapters Behind Existing Interfaces

### Goal
Swap persistence behind existing interfaces without changing controllers/services/use-cases, while honoring the “no stored analysis/recommendations/keywords” constraint.

### Exact changes
- Implement a Postgres-backed diary entry adapter that satisfies:
  - `DiaryEntryRepository` (`save`, `getByPath`, `deleteByPath`, `getAll`)
- Implement a Postgres-backed password verifier that satisfies:
  - `VerifyPasswordUserDataAccessInterface.verifyPassword()`
- Preserve current password semantics in DB mode:
  - If no user exists yet -> create default user with provided password and return `"Created new password."`
  - If password matches default user -> `"Correct Password"`
  - Else -> `"Incorrect Password"` (fail request)
- Wire beans via Spring profiles:
  - `file` profile: `DBNoteDataObject` + `VerifyPasswordDataAccessObject`
  - `postgres` profile: new Postgres adapters

StoragePath generation in DB mode:
- Keep `storagePath` as an opaque, stable string stored in `diary_entries.storage_path`.
- Recommended generation for new entries: `"db:" + <uuid>` (unique, no filesystem assumptions).

Keywords behavior (computed, not stored):
- Persist only: `title`, `text`, `created_at`, `updated_at`, `storage_path`, `user_id`.
- Compute keywords on demand for responses:
  - For `getAll()` and `getByPath()` results, compute keywords from `title + "\n\n" + text`.
  - If computation fails, return `[]` (do not fail the request).

### Files touched (planned)
- `backend/src/main/java/com/sentimentscribe/config/AppConfig.java`
- `backend/src/main/java/com/sentimentscribe/persistence/postgres/PostgresDiaryEntryRepositoryAdapter.java`
- `backend/src/main/java/com/sentimentscribe/persistence/postgres/PostgresVerifyPasswordDataAccessObject.java`
- (optional) `backend/src/main/java/com/sentimentscribe/persistence/postgres/StoragePathGenerator.java`

### Verification checklist
- Run backend in DB mode:
  - `SPRING_PROFILES_ACTIVE=postgres cd backend; mvn -q spring-boot:run`
- Manual API checks (must match current behavior):
  - `POST /api/auth/verify` with first-ever password returns `"Created new password."` and an entries list (likely empty initially).
  - Subsequent verify with same password returns `"Correct Password"`.
  - Verify with different password returns `400` with `"Incorrect Password"`.
  - `POST /api/entries` creates and returns a `storagePath`.
  - `GET /api/entries/by-path?path=<storagePath>` loads the same entry.
  - `GET /api/entries` includes the entry in the list (keywords present, computed on demand).
  - `DELETE /api/entries?path=<storagePath>` deletes it.
- DB validation (examples):
  - `select username, password from users;`
  - `select storage_path, title, created_at, updated_at from diary_entries;`

## Step 5 - Cutover + Cleanup (Remove File Storage)

### Goal
Make Postgres the default persistence and remove file-storage code/data once DB mode is verified.

### Exact changes
- Switch default runtime profile to `postgres` (deployment config / env var), or change config to prefer DB when datasource is configured.
- Remove file-based persistence code once unused:
  - Delete `DBNoteDataObject` and its tests
  - Delete `VerifyPasswordDataAccessObject` and its tests
- Remove the `diary_entry_database/*.json` fixtures (clean DB start; no importer/backfill exists in this plan).
- Update documentation to reflect Postgres as the persistence layer and clarify the clean start.

### Files touched (planned)
- Delete: `backend/src/main/java/com/sentimentscribe/data/DBNoteDataObject.java`
- Delete: `backend/src/test/java/com/sentimentscribe/data/DBNoteDataObjectTest.java`
- Delete: `backend/src/main/java/com/sentimentscribe/data/VerifyPasswordDataAccessObject.java`
- Delete: `backend/src/test/java/com/sentimentscribe/data/VerifyPasswordDataAccessObjectTest.java`
- Delete: `backend/src/main/java/com/sentimentscribe/data/diary_entry_database/*.json`
- Modify: `backend/src/main/java/com/sentimentscribe/config/AppConfig.java`
- Modify: `README.md`, `README-backend.md` (if needed)

### Verification checklist
- DB mode tests pass: `cd backend; mvn -q test`
- Manual smoke in DB mode:
  - verify password
  - list/create/load/delete entries
  - analyze keywords
  - recommendations endpoint
- DB sanity:
  - Ensure `users` and `diary_entries` reflect current app state after use.

## Cleanup Policy (What Gets Deleted When)

- After Step 4 is verified in DB mode (but before cutover):
  - Keep file storage code temporarily for fallback via `file` profile.
- After Step 5 cutover and verification:
  - Delete file storage implementation + tests:
    - `backend/src/main/java/com/sentimentscribe/data/DBNoteDataObject.java`
    - `backend/src/test/java/com/sentimentscribe/data/DBNoteDataObjectTest.java`
    - `backend/src/main/java/com/sentimentscribe/data/VerifyPasswordDataAccessObject.java`
    - `backend/src/test/java/com/sentimentscribe/data/VerifyPasswordDataAccessObjectTest.java`
  - Delete JSON fixtures/data under `backend/src/main/java/com/sentimentscribe/data/diary_entry_database/`
