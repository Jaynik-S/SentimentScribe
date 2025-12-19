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

## Step 3 – Spring Boot Becomes the Sole Runtime (Delete Desktop Runtime)

### Summary of changes made in this step
- (Fill in after completing Step 3)

### Key technical details and decisions
- Boot main class:
- Health endpoint:
- Desktop runtime removed (what was deleted):

### Files added / modified / deleted
- (List paths)

### Verification notes
- `mvn -q spring-boot:run`:
- Health endpoint response:

---

## Step 4 – Replace AppBuilder/Bootstrap With Spring Configuration

### Summary of changes made in this step
- (Fill in after completing Step 4)

### Key technical details and decisions
- Deleted builders/bootstrap classes:
- Spring-managed wiring approach (annotations/config):
- Error handling strategy (`@ControllerAdvice`, etc.):
- Service boundaries chosen (what became a `@Service`):

### Files added / modified / deleted
- (List paths)

### Verification notes
- Application starts and does not reference removed builders.

---

## Step 5 – Build the Backend API (Controllers → Services → Repositories)

### Summary of changes made in this step
- (Fill in after completing Step 5)

### Key technical details and decisions
- Controllers added:
- DTOs and mapping approach:
- Service layer responsibilities:
- Persistence implementation:
- Notes on concurrency/locking (if file-based persistence):

### Files added / modified / deleted
- (List paths)

### Verification notes
- Manual API smoke checks performed:

---

## Step 6 – Configuration: `application.yml` + Secrets + Profiles

### Summary of changes made in this step
- (Fill in after completing Step 6)

### Key technical details and decisions
- Configuration keys introduced:
- Secret handling approach (env vars, vault, etc.):
- Profiles introduced (if any):
- Removed config approaches:

### Files added / modified / deleted
- (List paths)

### Verification notes
- App starts with real config:
- Missing config behavior is clear:

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
