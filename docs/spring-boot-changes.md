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
- Spring Boot version: 3.5.9
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
- Moved all code under the `com.sentimentscribe` root package and updated package declarations/imports accordingly.
- Relocated diary entry JSON fixtures alongside the new data access package and updated their stored paths.

### Key technical details and decisions
- Root package chosen: `com.sentimentscribe`
- Packages moved: `app` → `com.sentimentscribe.app`, `use_case` → `com.sentimentscribe.usecase`, `interface_adapter` → `com.sentimentscribe.adapter`, `data_access` → `com.sentimentscribe.data`, `entity` → `com.sentimentscribe.domain`, `view` → `com.sentimentscribe.view`
- Any renames performed: package segments normalized (e.g., `use_case` → `usecase`, `interface_adapter` → `adapter`)
- Notes on how imports/tests were updated: package declarations and imports updated across `src/main/java` and `src/test/java` to the new root

### Files added / modified / deleted
- `src/main/java/com/sentimentscribe/**`
- `src/test/java/com/sentimentscribe/**`
- `src/main/java/com/sentimentscribe/data/diary_entry_database/*.json`

### Verification notes
- `mvn -q test` after refactor: failed (unhandled `org.json.JSONException` in `com.sentimentscribe.data.DBNoteDataObjectTest`)

---

## Step 3 - Spring Boot Becomes the Sole Runtime (Delete Desktop Runtime)

### Summary of changes made in this step
- Added the Spring Boot application entry point and a health endpoint for runtime validation.
- Removed the Swing main entry point, its UI-only test, and the Swing view package.

### Key technical details and decisions
- Boot main class: `com.sentimentscribe.SentimentScribeApplication`
- Health endpoint: `GET /api/health` → `{ "status": "ok" }`
- Desktop runtime removed (what was deleted): `MainNoteApplication`, `MainNoteApplicationTest`, `com.sentimentscribe.view/**`

### Files added / modified / deleted
- `src/main/java/com/sentimentscribe/SentimentScribeApplication.java`
- `src/main/java/com/sentimentscribe/web/HealthController.java`
- `src/main/java/com/sentimentscribe/app/MainNoteApplication.java` (deleted)
- `src/test/java/com/sentimentscribe/app/MainNoteApplicationTest.java` (deleted)
- `src/main/java/com/sentimentscribe/view/**` (deleted)

### Verification notes
- `mvn -q spring-boot:run`:
- Health endpoint response:

---

## Step 4 - Replace AppBuilder/Bootstrap With Spring Configuration

### Summary of changes made in this step
- Removed the Swing composition root and UI-only wiring helpers.
- Added Spring configuration beans for data access and external API adapters.

### Key technical details and decisions
- Deleted builders/bootstrap classes: `NoteAppBuilder`, `HomeMenuRefreshListener`, `com.sentimentscribe.adapter/**`
- Spring-managed wiring approach (annotations/config): `AppConfig` with constructor-injected `@Bean` definitions
- Error handling strategy (`@ControllerAdvice`, etc.): deferred (only health endpoint exists)
- Service boundaries chosen (what became a `@Service`): not introduced yet (use cases remain unchanged)

### Files added / modified / deleted
- `src/main/java/com/sentimentscribe/config/AppConfig.java`
- `src/main/java/com/sentimentscribe/app/NoteAppBuilder.java` (deleted)
- `src/main/java/com/sentimentscribe/app/HomeMenuRefreshListener.java` (deleted)
- `src/main/java/com/sentimentscribe/adapter/**` (deleted)

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
- DTOs and mapping approach: request/response DTOs under `com.sentimentscribe.web.dto`, mapped from use case output data and persisted entry summaries
- Service layer responsibilities: services invoke existing interactors (save/load/delete/analyze/recommendations/verify password) and return structured results for controllers
- Persistence implementation: `DiaryEntryRepository` interface backed by `DBNoteDataObject` (file-based JSON)
- Notes on concurrency/locking (if file-based persistence): unchanged (current file IO is single-process, no explicit locking)

### Files added / modified / deleted
- `src/main/java/com/sentimentscribe/web/EntriesController.java`
- `src/main/java/com/sentimentscribe/web/AnalysisController.java`
- `src/main/java/com/sentimentscribe/web/RecommendationsController.java`
- `src/main/java/com/sentimentscribe/web/AuthController.java`
- `src/main/java/com/sentimentscribe/web/dto/**`
- `src/main/java/com/sentimentscribe/service/**`
- `src/main/java/com/sentimentscribe/data/DiaryEntryRepository.java`
- `src/main/java/com/sentimentscribe/data/DBNoteDataObject.java`
- `src/main/java/com/sentimentscribe/usecase/create_entry/**` (deleted)
- `src/main/java/com/sentimentscribe/usecase/go_back/**` (deleted)
- `src/test/java/com/sentimentscribe/usecase/create_entry/**` (deleted)
- `src/test/java/com/sentimentscribe/usecase/go_back/**` (deleted)

### Verification notes
- Manual API smoke checks performed: not run

---

## Step 6 – Configuration: `application.yml` + Secrets + Profiles

### Summary of changes made in this step
- Added Spring Boot configuration in application.yml with env-backed secrets, CORS, and logging settings.
- Introduced typed configuration properties for external APIs and moved data access classes off dotenv.
- Removed dotenv usage and the repository .env file; updated related tests and wiring.

### Key technical details and decisions
- Configuration keys introduced: `server.port`, `server.servlet.context-path`, `sentimentscribe.spotify.client-id`, `sentimentscribe.spotify.client-secret`, `sentimentscribe.tmdb.api-key`, `sentimentscribe.auth.password`, `sentimentscribe.cors.allowed-origins`, `logging.level.com.sentimentscribe.data`
- Secret handling approach (env vars, vault, etc.): environment variables via application.yml placeholders
- Profiles introduced (if any): none
- Removed config approaches: dotenv-java + .env file; Dotenv usage removed from data access classes

### Files added / modified / deleted
- `src/main/resources/application.yml`
- `src/main/java/com/sentimentscribe/config/SpotifyProperties.java`
- `src/main/java/com/sentimentscribe/config/TmdbProperties.java`
- `src/main/java/com/sentimentscribe/config/AuthProperties.java`
- `src/main/java/com/sentimentscribe/config/CorsProperties.java`
- `src/main/java/com/sentimentscribe/config/WebConfig.java`
- `src/main/java/com/sentimentscribe/SentimentScribeApplication.java`
- `src/main/java/com/sentimentscribe/config/AppConfig.java`
- `src/main/java/com/sentimentscribe/data/SpotifyAPIAccessObject.java`
- `src/main/java/com/sentimentscribe/data/TMDbAPIAccessObject.java`
- `src/main/java/com/sentimentscribe/data/RecommendationAPIAccessObject.java`
- `src/main/java/com/sentimentscribe/data/VerifyPasswordDataAccessObject.java`
- `src/test/java/com/sentimentscribe/data/SpotifyAPIAccessObjectTest.java`
- `src/test/java/com/sentimentscribe/data/TMDbAPIAccessObjectTest.java`
- `src/test/java/com/sentimentscribe/data/VerifyPasswordDataAccessObjectTest.java`
- `pom.xml`
- `.env` (deleted)

### Verification notes
- App starts with real config: not run
- Missing config behavior is clear: not run

---

## Step 7 - Backend Tests: Context Load, Controller Smoke Test, Integration

### Summary of changes made in this step
- Added Spring Boot context load and controller smoke tests.
- Added an entries API integration test covering a happy path and a failure path.

### Key technical details and decisions
- Context load test: `SentimentScribeApplicationContextTest`
- Controller smoke test approach (`MockMvc` / `TestRestTemplate`): `HealthControllerTest` uses `@WebMvcTest` + `MockMvc`; entries integration uses `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `TestRestTemplate`
- Integration test coverage: create/load/list entries via REST plus missing-path error case
- Backend-independence checks (no desktop dependencies): tests run against Spring context and REST endpoints only

### Files added / modified / deleted
- `src/test/java/com/sentimentscribe/SentimentScribeApplicationContextTest.java`
- `src/test/java/com/sentimentscribe/web/HealthControllerTest.java`
- `src/test/java/com/sentimentscribe/web/EntriesApiIntegrationTest.java`

### Verification notes
- `mvn -q test`: PASS
- Backend runs independently: verified via `mvn -q spring-boot:run`

---

## Stabilization & Bug Fixes

### Summary of major issues found
- Duplicate org.json implementations on the test classpath caused checked `JSONException` compile errors and runtime `NoSuchMethodError`.
- REST integration tests used a missing annotation and repository types that did not match use-case interactor signatures.
- Web slice tests failed due to missing configuration properties beans.

### Categories of fixes (packaging, DI, controllers, tests, config)
- Packaging/dependencies: excluded `android-json` from test scope to force the modern org.json implementation.
- DI/configuration: enabled CORS properties binding for MVC config to load in slices and full context.
- Controllers/services: aligned repository contract with use-case data access interfaces to restore interactor wiring.
- Tests: updated integration tests to use `@LocalServerPort` + explicit `TestRestTemplate` base URL.

### Architectural corrections made
- Repository interface now extends the use-case data access contracts so services can invoke interactors without adapter shims.

### Files deleted because they were invalid or unused
- None

---

## Final Validation Pass

### Commands run and results
- `mvn -q test`: PASS
- `mvn -q package`: PASS
- `mvn -q spring-boot:run`: SUCCESS
- `GET /api/health`: 200
- `GET /api/entries`: 200

### Fixes applied (build, tests, DI, endpoints, config, cleanup)
- Build: removed unused dependencies (`spring-boot-starter-validation`, `spring-boot-starter-actuator`, `okhttp`).
- Config/docs: updated README to reflect environment-variable configuration.
- Cleanup: validated no remaining references to deleted Swing packages or adapter classes.

### Known limitations
- Recommendation endpoints require `SPOTIFY_CLIENT_ID`, `SPOTIFY_CLIENT_SECRET`, and `TMDB_API_KEY` to be set; missing values will cause API calls to fail fast.

