# Spring Boot Migration Plan (Option A: Web Backend) — MoodVerse / DiaryDiscovery

> Change documentation rule: record step-by-step implementation notes **only** in `spring-boot-changes.md`.  
> Do **not** create per-step change files (no `Step-XX-Changes.md`).

## Table of Contents

- [Step 0 — Commit to Option A (Web Backend) + Define Target API](#step-0--commit-to-option-a-web-backend--define-target-api)
- [Step 1 — `pom.xml`: Spring Boot Parent, Starters, Plugins](#step-1--pomxml-spring-boot-parent-starters-plugins)
- [Step 2 — Package Restructuring for Spring Boot Component Scanning](#step-2--package-restructuring-for-spring-boot-component-scanning)
- [Step 3 — Spring Boot Becomes the Sole Runtime (Delete Desktop Runtime)](#step-3--spring-boot-becomes-the-sole-runtime-delete-desktop-runtime)
- [Step 4 — Replace AppBuilder/Bootstrap With Spring Configuration](#step-4--replace-appbuilderbootstrap-with-spring-configuration)
- [Step 5 — Build the Backend API (Controllers → Services → Repositories)](#step-5--build-the-backend-api-controllers--services--repositories)
- [Step 6 — Configuration: `application.yml` + Secrets + Profiles](#step-6--configuration-applicationyml--secrets--profiles)
- [Step 7 — Backend Tests: Context Load, Controller Smoke Test, Integration](#step-7--backend-tests-context-load-controller-smoke-test-integration)

---

## Step 0 — Commit to a Web Backend + Define Target API

**Goal**
Lock in the web-backend target architecture and define the minimal API surface that replaces the desktop UI workflows.

**Exactly what to change/add**
- [ ] Confirm the application will become a **web application** with a **Spring Boot backend** (Option A only).
- [ ] Define the initial REST API endpoints (minimal set to migrate desktop flows):
  - [ ] Auth/session: login / verify password (or replacement auth strategy).
  - [ ] Diary entries: create, list, load, update, delete.
  - [ ] Analysis: analyze keywords/sentiment.
  - [ ] Recommendations: songs + movies for an entry.
- [ ] Decide persistence for the first web iteration (pick one; avoid mixing):
  - [ ] **File-based** (fastest migration from current implementation if file IO is already used)
  - [ ] **Database** (recommended longer-term; requires schema + migration strategy)
- [ ] Decide how the frontend will exist (plan-level only; backend steps remain the same):
  - [ ] Separate SPA (React/Vue/etc.) calling the backend
  - [ ] Server-rendered views (Thymeleaf)

**Files to create/modify**
- Modify: `spring-boot-changes.md` (append notes under “Step 0”)
- Modify: `README.md` (add a short “Target architecture / API” section)

**How to verify**
- [ ] `README.md` clearly states “Spring Boot web backend” and the initial endpoint list.
- [ ] A single source of truth exists for the target API and persistence choice.

---

## Step 1 — `pom.xml`: Spring Boot Parent, Starters, Plugins

**Goal**
Convert the build to Spring Boot 3.x conventions for a web backend.

**Exactly what to change/add**
- [ ] Switch to `spring-boot-starter-parent` (Boot 3.x) and keep Java at **21**.
- [ ] Add starters aligned with a web backend:
  - [ ] `spring-boot-starter-web`
  - [ ] `spring-boot-starter-validation`
  - [ ] `spring-boot-starter-actuator` (recommended for health checks)
  - [ ] `spring-boot-starter-test` (test framework baseline)
- [ ] Add `spring-boot-maven-plugin` (build runnable jars and standardize packaging).
- [ ] Remove dependencies that are no longer needed in a Boot setup:
  - [ ] `org.junit.jupiter:junit-jupiter` (covered by `spring-boot-starter-test`)
  - [ ] `org.slf4j:slf4j-simple` (Boot provides logging)
- [ ] Keep existing non-Spring dependencies that are still used by backend logic (CoreNLP, OkHttp, JSON) unless/until replaced.

**Files to create/modify**
- Modify: `pom.xml`
- Modify: `spring-boot-changes.md` (append notes under “Step 1”)

**How to verify**
- [ ] `mvn -q test` passes.
- [ ] `mvn -q package` succeeds.

---

## Step 2 — Package Restructuring for Spring Boot Component Scanning

**Goal**
Move code under a single root package so Spring Boot component scanning, configuration, and tests work cleanly.

**Exactly what to change/add**
- [ ] Choose a root package and apply it consistently (example: `com.moodverse`).
- [ ] Move current packages under the root (use IDE refactor; keep logical boundaries):
  - [ ] `app` → `com.moodverse.app`
  - [ ] `use_case` → `com.moodverse.usecase`
  - [ ] `interface_adapter` → `com.moodverse.adapter` (or `com.moodverse.web` once controller layer exists)
  - [ ] `data_access` → `com.moodverse.data`
  - [ ] `entity` → `com.moodverse.domain`
- [ ] Update test packages under `src/test/java` to match.
- [ ] Remove any empty directories created by moves (do not keep unused package folders).

**Files to create/modify**
- Modify: `src/main/java/**` (package declarations + moved files)
- Modify: `src/test/java/**` (package declarations + moved files)
- Modify: `spring-boot-changes.md` (append notes under “Step 2”)

**How to verify**
- [ ] `mvn -q test` passes after the refactor.
- [ ] No duplicate packages remain under the old roots (`app`, `use_case`, etc.).

---

## Step 3 — Spring Boot Becomes the Sole Runtime (Delete Desktop Runtime)

**Goal**
Make Spring Boot the only way the application runs, as a web backend.

**Exactly what to change/add**
- [ ] Create the Spring Boot application entry point:
  - [ ] `@SpringBootApplication` main class under the chosen root package.
- [ ] Add a minimal HTTP endpoint to validate the runtime:
  - [ ] `GET /api/health` (or Actuator `GET /actuator/health` if enabled)
- [ ] Delete the desktop runtime entry points and UI-only tests:
  - [ ] Delete the Swing main class (current `MainNoteApplication`).
  - [ ] Delete Swing-specific tests that only validate UI builders/frames.
- [ ] Delete desktop UI packages that are not used by the backend (remove immediately once no longer referenced):
  - [ ] `view/**` (Swing UI views)
  - [ ] any Swing-specific “view manager” / UI controllers not needed for REST

**Files to create/modify**
- Create: `src/main/java/com/moodverse/MoodVerseApplication.java`
- Create: `src/main/java/com/moodverse/web/HealthController.java` (or equivalent)
- Delete: `src/main/java/**/MainNoteApplication.java`
- Delete: `src/test/java/**/MainNoteApplicationTest.java`
- Delete: `src/main/java/**/view/**`
- Modify: `spring-boot-changes.md` (append notes under “Step 3”)

**How to verify**
- [ ] `mvn -q test` passes.
- [ ] `mvn -q spring-boot:run` starts successfully.
- [ ] `GET /api/health` (or `GET /actuator/health`) returns `200`.

---

## Step 4 — Replace AppBuilder/Bootstrap With Spring Configuration

**Goal**
Remove any remaining custom bootstrapping/builders and have Spring fully manage application wiring.

**Exactly what to change/add**
- [ ] Delete all non-Spring application builders/composition roots (example: `NoteAppBuilder`).
- [ ] Create Spring-managed beans using constructor injection:
  - [ ] repositories / data access components
  - [ ] services (business logic orchestration)
  - [ ] adapters around external APIs (Spotify/TMDb)
- [ ] If the current “use case interactors” are still used:
  - [ ] Convert them into `@Service` classes, or wrap them behind service interfaces.
  - [ ] Remove presenter/view-model patterns that only existed for Swing UI.
- [ ] Add centralized error handling for the web layer (`@ControllerAdvice`) as soon as the first real endpoints exist.

**Files to create/modify**
- Delete: `src/main/java/**/NoteAppBuilder.java`
- Create: `src/main/java/com/moodverse/config/**` (Spring configuration as needed)
- Modify: `src/main/java/com/moodverse/**` (constructors and wiring updates)
- Modify: `spring-boot-changes.md` (append notes under “Step 4”)

**How to verify**
- [ ] Application starts with `mvn -q spring-boot:run` without referencing deleted builder classes.
- [ ] Health endpoint still returns `200`.

---

## Step 5 — Build the Backend API (Controllers → Services → Repositories)

**Goal**
Expose the core application functionality through a REST API backed by Spring-managed services.

**Exactly what to change/add**
- [ ] Define REST controllers aligned with Step 0 endpoint list:
  - [ ] entries: create/list/get/update/delete
  - [ ] analysis: analyze content
  - [ ] recommendations: fetch songs/movies
- [ ] Introduce DTOs for requests/responses (do not expose domain entities directly).
- [ ] Implement a service layer that orchestrates existing domain logic/use cases.
- [ ] Implement repositories for persistence:
  - [ ] File-based repository (if chosen in Step 0), or
  - [ ] DB repository (if chosen in Step 0; include schema/migration approach in this same step)
- [ ] Delete any remaining packages/classes that were only used by the Swing flow and are now replaced by REST controllers.

**Files to create/modify**
- Create: `src/main/java/com/moodverse/web/**` (controllers)
- Create: `src/main/java/com/moodverse/web/dto/**`
- Create: `src/main/java/com/moodverse/service/**`
- Create/Modify: `src/main/java/com/moodverse/data/**`
- Modify: `spring-boot-changes.md` (append notes under “Step 5”)

**How to verify**
- [ ] `mvn -q spring-boot:run` starts.
- [ ] Manual smoke checks (curl/Postman) succeed for at least:
  - [ ] create entry
  - [ ] list entries
  - [ ] get recommendations

---

## Step 6 — Configuration: `application.yml` + Secrets + Profiles

**Goal**
Move all backend configuration into Spring Boot configuration (no desktop-era config paths remain).

**Exactly what to change/add**
- [ ] Add `application.yml` (or `application.properties`) and define:
  - [ ] server port + context path (if needed)
  - [ ] Spotify/TMDb credentials
  - [ ] CORS configuration (if you have a separate frontend)
  - [ ] logging levels for external API integrations
- [ ] Introduce typed config with `@ConfigurationProperties` for external APIs.
- [ ] Remove config approaches that are no longer used:
  - [ ] Remove `dotenv-java` if you move entirely to environment variables / Spring config.
  - [ ] Delete `.env` from the repository workspace if it is no longer part of the runtime setup.
- [ ] Add profile-specific config only if required (e.g., `application-test.yml`).

**Files to create/modify**
- Create: `src/main/resources/application.yml`
- Create (optional): `src/test/resources/application-test.yml`
- Modify (optional): `pom.xml` (remove `dotenv-java` dependency if unused)
- Delete (if unused): `.env`
- Modify: `spring-boot-changes.md` (append notes under “Step 6”)

**How to verify**
- [ ] App starts with config provided via environment variables and/or `application.yml`.
- [ ] Missing secrets fail clearly (startup or first call) with actionable errors.

---

## Step 7 — Backend Tests: Context Load, Controller Smoke Test, Integration

**Goal**
Add Spring Boot tests that prove the backend boots and the API works end-to-end at a basic level.

**Exactly what to change/add**
- [ ] Add a context load test:
  - [ ] `@SpringBootTest` verifies the Spring application context starts.
- [ ] Add at least one controller/API smoke test (pick one):
  - [ ] `@WebMvcTest` + `MockMvc` against a controller, or
  - [ ] `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `TestRestTemplate` hitting `/api/health` and one real API route.
- [ ] Add backend-focused integration tests aligned with the web architecture:
  - [ ] Controller → service → repository integration for one “happy path”
  - [ ] One failure path (e.g., missing entry id returns `404`)
- [ ] Validate the backend runs independently (no UI code, no desktop bootstrapping):
  - [ ] Ensure tests and runtime do not reference deleted Swing packages/classes.

**Files to create/modify**
- Create: `src/test/java/com/moodverse/MoodVerseApplicationContextTest.java`
- Create: `src/test/java/com/moodverse/web/HealthControllerTest.java` (or another controller smoke test)
- Create: `src/test/java/com/moodverse/web/EntriesApiIntegrationTest.java` (example)
- Modify: `spring-boot-changes.md` (append notes under “Step 7”)

**How to verify**
- [ ] `mvn -q test` passes.
- [ ] `mvn -q spring-boot:run` starts and serves HTTP endpoints without any desktop/UI dependencies.
