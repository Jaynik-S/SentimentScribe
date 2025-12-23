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
- (fill in)

### Exact changes made
- (fill in)

### Files touched (actual)
- (fill in)

### Verification notes
- Commands run:
  - (fill in)

---

## Step 4 - Implement Postgres Adapters Behind Existing Interfaces

### Summary
- (fill in)

### Exact changes made
- (fill in)

### Files touched (actual)
- (fill in)

### Verification notes
- Commands run:
  - (fill in)
- Manual API checks (record outcomes):
  - (fill in)
- DB checks (record queries + results):
  - (fill in)

---

## Step 5 - Cutover + Cleanup (Remove File Storage)

### Summary
- (fill in)

### Exact changes made
- (fill in)

### Files touched (actual)
- (fill in)

### Deleted (confirm removed files/directories)
- (fill in)

### Verification notes
- Commands run:
  - (fill in)
- Manual smoke checks:
  - (fill in)
