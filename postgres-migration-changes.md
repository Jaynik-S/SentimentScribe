# PostgreSQL Migration Change Log (Template)

> Use this file to record what was actually changed when executing `postgres-migration-plan.md`.
> This is a template only - no implementation has been performed yet.

## Step 0 - Freeze Baseline + Scope Decisions

### Summary
- Documented Step 0 decisions and recorded completion status. No code changes.

### Exact changes made
- Added a Step 0 completion checklist to the plan to lock the baseline decisions.
- Recorded Step 0 execution details in this change log.

### Files touched (actual)
- `postgres-migration-plan.md`
- `postgres-migration-changes.md`

### Verification notes
- Commands run:
  - Not run (doc-only).
- Manual checks:
  - Not run (doc-only).

---

## Step 1 - Add PostgreSQL + Flyway + Local Docker

### Summary
- Added Postgres dependencies, profile config, and a local Docker Compose service.

### Exact changes made
- Added JPA, Postgres driver, Flyway, and Testcontainers dependencies in `backend/pom.xml`.
- Added `backend/src/main/resources/application-postgres.yml` for DB profile settings.
- Added root `docker-compose.yml` with a local Postgres service and volume.
- Documented local Postgres run notes in the root README.

### Files touched (actual)
- `backend/pom.xml`
- `backend/src/main/resources/application-postgres.yml`
- `docker-compose.yml`
- `README.md`

### Verification notes
- Commands run:
  - Not run (not requested).
- Manual checks:
  - Not run (not requested).

---

## Step 2 - Create Flyway Migrations (Schema)

### Summary
- Added initial Flyway schema for users and diary entries.

### Exact changes made
- Created `V1__init.sql` with `users` and `diary_entries` tables.
- Included UNIQUE constraint on `diary_entries.storage_path` and FK to `users`.

### Files touched (actual)
- `backend/src/main/resources/db/migration/V1__init.sql`

### Verification notes
- Commands run:
  - Not run (not requested).
- SQL checks:
  - Not run (not requested).

---

## Step 3 - Add JPA Entities + Spring Data Repositories

### Summary
- Added Postgres JPA entities and repositories for users and diary entries.

### Exact changes made
- Created `UserEntity` and `DiaryEntryEntity` with UUID IDs, timestamps, and the user relation.
- Added `UserJpaRepository` and `DiaryEntryJpaRepository` interfaces.

### Files touched (actual)
- `backend/src/main/java/com/sentimentscribe/persistence/postgres/entity/UserEntity.java`
- `backend/src/main/java/com/sentimentscribe/persistence/postgres/entity/DiaryEntryEntity.java`
- `backend/src/main/java/com/sentimentscribe/persistence/postgres/repo/UserJpaRepository.java`
- `backend/src/main/java/com/sentimentscribe/persistence/postgres/repo/DiaryEntryJpaRepository.java`

### Verification notes
- Commands run:
  - Not run (not requested).

---

## Step 4 - Implement Postgres Adapters Behind Existing Interfaces

### Summary
- Added Postgres adapters for diary entries and password verification, wired behind profiles.

### Exact changes made
- Implemented `PostgresDiaryEntryRepositoryAdapter` with storagePath generation and on-demand keyword extraction.
- Implemented `PostgresVerifyPasswordDataAccessObject` with default user creation and password semantics.
- Added `StoragePathGenerator` helper for `db:` identifiers.
- Wired file vs. postgres adapters via Spring profiles in `AppConfig`.
- Added lookup methods to JPA repositories for `storagePath` and `username`.

### Files touched (actual)
- `backend/src/main/java/com/sentimentscribe/persistence/postgres/PostgresDiaryEntryRepositoryAdapter.java`
- `backend/src/main/java/com/sentimentscribe/persistence/postgres/PostgresVerifyPasswordDataAccessObject.java`
- `backend/src/main/java/com/sentimentscribe/persistence/postgres/StoragePathGenerator.java`
- `backend/src/main/java/com/sentimentscribe/config/AppConfig.java`
- `backend/src/main/java/com/sentimentscribe/persistence/postgres/repo/DiaryEntryJpaRepository.java`
- `backend/src/main/java/com/sentimentscribe/persistence/postgres/repo/UserJpaRepository.java`

### Verification notes
- Commands run:
  - Not run (not requested).
- Manual API checks (record outcomes):
  - Not run (not requested).
- DB checks (record queries + results):
  - Not run (not requested).

---

## Step 5 - Cutover + Cleanup (Remove File Storage)

### Summary
- Cut over to Postgres as default and removed file-based persistence code, data, and tests.

### Exact changes made
- Removed file-based persistence classes and wiring.
- Set the default Spring profile to `postgres`.
- Updated backend docs for Postgres-first behavior and clean-start persistence.
- Updated Spring Boot tests to use Postgres via Testcontainers.
- Deleted legacy JSON fixture files.

### Files touched (actual)
- `backend/src/main/java/com/sentimentscribe/config/AppConfig.java`
- `backend/src/main/resources/application.yml`
- `backend/src/test/java/com/sentimentscribe/SentimentScribeApplicationContextTest.java`
- `backend/src/test/java/com/sentimentscribe/web/EntriesApiIntegrationTest.java`
- `README.md`
- `README-backend.md`
- `postgres-migration-changes.md`

### Deleted (confirm removed files/directories)
- `backend/src/main/java/com/sentimentscribe/data/DBNoteDataObject.java`
- `backend/src/main/java/com/sentimentscribe/data/VerifyPasswordDataAccessObject.java`
- `backend/src/test/java/com/sentimentscribe/data/DBNoteDataObjectTest.java`
- `backend/src/test/java/com/sentimentscribe/data/VerifyPasswordDataAccessObjectTest.java`
- `backend/src/main/java/com/sentimentscribe/data/diary_entry_database/`

### Verification notes
- Commands run:
  - Not run (not requested).
- Manual smoke checks:
  - Not run (not requested).
