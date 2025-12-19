# Spring Boot Migration Change Log

This is the **only** place to record step-by-step implementation changes during the migration described in `SpringBoot.md`.

---

## Step 0 - Commit to Option A (Web Backend) + Define Target API

### Summary of changes made in this step
- Committed to a Spring Boot web backend and documented the initial REST API, persistence choice, and frontend approach in the README.

### Key technical details and decisions
- Target architecture: Spring Boot web backend
- Initial API endpoints selected: auth/session (login, verify password), diary entries CRUD, analysis (keywords/sentiment), recommendations (songs + movies)
- Persistence choice selected: file-based storage
- Frontend approach selected: separate SPA consuming the backend API

### Files added / modified / deleted
- `README.md`
- `spring-boot-changes.md`

### Verification notes
- Documentation updated and consistent

---

## Step 1 - `pom.xml`: Spring Boot Parent, Starters, Plugins

### Summary of changes made in this step
- Switched to Spring Boot parent POM, added web/validation/actuator/test starters, and added the Boot Maven plugin.
- Removed standalone JUnit and SLF4J Simple dependencies while retaining existing backend libraries (CoreNLP, OkHttp, JSON, dotenv).

### Key technical details and decisions
- Spring Boot version: 3.3.5
- Java version: 21
- Starters added: `spring-boot-starter-web`, `spring-boot-starter-validation`, `spring-boot-starter-actuator`, `spring-boot-starter-test`
- Dependencies removed: `org.junit.jupiter:junit-jupiter`, `org.slf4j:slf4j-simple`
- Maven plugin changes: added `spring-boot-maven-plugin`
- Actuator exposure decision (public/protected): not configured yet (defaults apply)

### Files added / modified / deleted
- `pom.xml`

### Verification notes
- `mvn -q test`: failed (test compile errors: unhandled `org.json.JSONException` in `DBNoteDataObjectTest`)
- `mvn -q package`: failed (tests: unhandled `org.json.JSONException` in data_access tests; `NoSuchMethodError` for `org.json.JSONObject.put(String, Collection)` in `DBNoteDataObjectTest`)

---

## Step 2 - Package Restructuring for Spring Boot Component Scanning

### Summary of changes made in this step
- Moved all code under the `com.moodverse` root package and updated package declarations/imports accordingly.
- Relocated diary entry JSON fixtures alongside the new data access package and updated their stored paths.

### Key technical details and decisions
- Root package chosen: `com.moodverse`
- Packages moved: `app` → `com.moodverse.app`, `use_case` → `com.moodverse.usecase`, `interface_adapter` → `com.moodverse.adapter`, `data_access` → `com.moodverse.data`, `entity` → `com.moodverse.domain`, `view` → `com.moodverse.view`
- Any renames performed: package segments normalized (e.g., `use_case` → `usecase`, `interface_adapter` → `adapter`)
- Notes on how imports/tests were updated: package declarations and imports updated across `src/main/java` and `src/test/java` to the new root

### Files added / modified / deleted
- `src/main/java/com/moodverse/**`
- `src/test/java/com/moodverse/**`
- `src/main/java/com/moodverse/data/diary_entry_database/*.json`

### Verification notes
- `mvn -q test` after refactor: failed (unhandled `org.json.JSONException` in `com.moodverse.data.DBNoteDataObjectTest`)

---

## Step 3 - Spring Boot Becomes the Sole Runtime (Delete Desktop Runtime)

### Summary of changes made in this step
- Added the Spring Boot application entry point and a health endpoint for runtime validation.
- Removed the Swing main entry point, its UI-only test, and the Swing view package.

### Key technical details and decisions
- Boot main class: `com.moodverse.MoodVerseApplication`
- Health endpoint: `GET /api/health` → `{ "status": "ok" }`
- Desktop runtime removed (what was deleted): `MainNoteApplication`, `MainNoteApplicationTest`, `com.moodverse.view/**`

### Files added / modified / deleted
- `src/main/java/com/moodverse/MoodVerseApplication.java`
- `src/main/java/com/moodverse/web/HealthController.java`
- `src/main/java/com/moodverse/app/MainNoteApplication.java` (deleted)
- `src/test/java/com/moodverse/app/MainNoteApplicationTest.java` (deleted)
- `src/main/java/com/moodverse/view/**` (deleted)

### Verification notes
- `mvn -q spring-boot:run`:
- Health endpoint response:

---

## Step 4 - Replace AppBuilder/Bootstrap With Spring Configuration

### Summary of changes made in this step
- Removed the Swing composition root and UI-only wiring helpers.
- Added Spring configuration beans for data access and external API adapters.

### Key technical details and decisions
- Deleted builders/bootstrap classes: `NoteAppBuilder`, `HomeMenuRefreshListener`, `com.moodverse.adapter/**`
- Spring-managed wiring approach (annotations/config): `AppConfig` with constructor-injected `@Bean` definitions
- Error handling strategy (`@ControllerAdvice`, etc.): deferred (only health endpoint exists)
- Service boundaries chosen (what became a `@Service`): not introduced yet (use cases remain unchanged)

### Files added / modified / deleted
- `src/main/java/com/moodverse/config/AppConfig.java`
- `src/main/java/com/moodverse/app/NoteAppBuilder.java` (deleted)
- `src/main/java/com/moodverse/app/HomeMenuRefreshListener.java` (deleted)
- `src/main/java/com/moodverse/adapter/**` (deleted)

### Verification notes
- Application starts and does not reference removed builders.

---

## Step 5 – Build the Backend API (Controllers → Services → Repositories)

### Summary of changes made in this step
- Added REST controllers and DTOs for entries, analysis, recommendations, and auth/session flows.
- Implemented service layer orchestration around existing use cases and added a file-based repository interface.
- Removed remaining Swing-only use case packages/tests that are superseded by the REST API.

### Key technical details and decisions
- Controllers added: `EntriesController`, `AnalysisController`, `RecommendationsController`, `AuthController`
- DTOs and mapping approach: request/response DTOs under `com.moodverse.web.dto`, mapped from use case output data and persisted entry summaries
- Service layer responsibilities: services invoke existing interactors (save/load/delete/analyze/recommendations/verify password) and return structured results for controllers
- Persistence implementation: `DiaryEntryRepository` interface backed by `DBNoteDataObject` (file-based JSON)
- Notes on concurrency/locking (if file-based persistence): unchanged (current file IO is single-process, no explicit locking)

### Files added / modified / deleted
- `src/main/java/com/moodverse/web/EntriesController.java`
- `src/main/java/com/moodverse/web/AnalysisController.java`
- `src/main/java/com/moodverse/web/RecommendationsController.java`
- `src/main/java/com/moodverse/web/AuthController.java`
- `src/main/java/com/moodverse/web/dto/**`
- `src/main/java/com/moodverse/service/**`
- `src/main/java/com/moodverse/data/DiaryEntryRepository.java`
- `src/main/java/com/moodverse/data/DBNoteDataObject.java`
- `src/main/java/com/moodverse/usecase/create_entry/**` (deleted)
- `src/main/java/com/moodverse/usecase/go_back/**` (deleted)
- `src/test/java/com/moodverse/usecase/create_entry/**` (deleted)
- `src/test/java/com/moodverse/usecase/go_back/**` (deleted)

### Verification notes
- Manual API smoke checks performed: not run

---

## Step 6 – Configuration: `application.yml` + Secrets + Profiles

### Summary of changes made in this step
- Added Spring Boot configuration in application.yml with env-backed secrets, CORS, and logging settings.
- Introduced typed configuration properties for external APIs and moved data access classes off dotenv.
- Removed dotenv usage and the repository .env file; updated related tests and wiring.

### Key technical details and decisions
- Configuration keys introduced: `server.port`, `server.servlet.context-path`, `moodverse.spotify.client-id`, `moodverse.spotify.client-secret`, `moodverse.tmdb.api-key`, `moodverse.auth.password`, `moodverse.cors.allowed-origins`, `logging.level.com.moodverse.data`
- Secret handling approach (env vars, vault, etc.): environment variables via application.yml placeholders
- Profiles introduced (if any): none
- Removed config approaches: dotenv-java + .env file; Dotenv usage removed from data access classes

### Files added / modified / deleted
- `src/main/resources/application.yml`
- `src/main/java/com/moodverse/config/SpotifyProperties.java`
- `src/main/java/com/moodverse/config/TmdbProperties.java`
- `src/main/java/com/moodverse/config/AuthProperties.java`
- `src/main/java/com/moodverse/config/CorsProperties.java`
- `src/main/java/com/moodverse/config/WebConfig.java`
- `src/main/java/com/moodverse/MoodVerseApplication.java`
- `src/main/java/com/moodverse/config/AppConfig.java`
- `src/main/java/com/moodverse/data/SpotifyAPIAccessObject.java`
- `src/main/java/com/moodverse/data/TMDbAPIAccessObject.java`
- `src/main/java/com/moodverse/data/RecommendationAPIAccessObject.java`
- `src/main/java/com/moodverse/data/VerifyPasswordDataAccessObject.java`
- `src/test/java/com/moodverse/data/SpotifyAPIAccessObjectTest.java`
- `src/test/java/com/moodverse/data/TMDbAPIAccessObjectTest.java`
- `src/test/java/com/moodverse/data/VerifyPasswordDataAccessObjectTest.java`
- `pom.xml`
- `.env` (deleted)

### Verification notes
- App starts with real config: not run
- Missing config behavior is clear: not run

---

## Step 7 – Backend Tests: Context Load, Controller Smoke Test, Integration

### Summary of changes made in this step
- (Fill in after completing Step 7)

### Key technical details and decisions
- Context load test:
- Controller smoke test approach (`MockMvc` / `TestRestTemplate`):
- Integration test coverage:
- Backend-independence checks (no desktop dependencies):

### Files added / modified / deleted
- (List paths)

### Verification notes
- `mvn -q test`:
- Backend runs independently:
