# SentimentScribe Backend (Spring Boot) — Repo-Specific Guide

Audience: you know Java, but are new to Spring Boot. This document explains how *this repository’s* backend works end-to-end (no generic Spring tutorials).

---

## 1) Project Overview (what the backend does)

This backend is a Spring Boot REST API for a diary app. It provides endpoints to:

- Verify a simple “password gate” and return a list of entries.
- Create / update / load / delete diary entries stored in PostgreSQL.
- Analyze diary text into keywords using Stanford CoreNLP.
- Fetch music and movie recommendations by calling Spotify and TMDb, using extracted keywords.

The codebase keeps a Clean Architecture-style core:

- `com.sentimentscribe.domain` holds the core entities.
- `com.sentimentscribe.usecase.*` holds the “interactors” (business rules + validation) and boundary interfaces.
- Spring Boot is used mainly for runtime concerns: starting the server, wiring dependencies (DI), and HTTP request handling.

---

## 2) Directory/Package Map (what each folder/package contains)

Top-level backend locations in this repo:

- `src/main/java/com/sentimentscribe/SentimentScribeApplication.java`: Spring Boot entry point.
- `src/main/java/com/sentimentscribe/config/*`: Spring configuration + `application.yml` binding.
- `src/main/java/com/sentimentscribe/web/*`: REST controllers (HTTP → service calls).
- `src/main/java/com/sentimentscribe/web/dto/*`: request/response DTOs (JSON shapes).
- `src/main/java/com/sentimentscribe/service/*`: Spring `@Service` layer that orchestrates use cases per request.
- `src/main/java/com/sentimentscribe/usecase/*`: interactors + boundaries + port interfaces (framework-agnostic core).
- `src/main/java/com/sentimentscribe/data/*`: NLP + external API adapters.
- `src/main/java/com/sentimentscribe/persistence/postgres/*`: Postgres entities, repositories, and persistence adapters.
- `src/main/java/com/sentimentscribe/domain/*`: domain entities/value objects.
- `src/main/resources/application.yml`: runtime configuration (ports, CORS, API keys).

---

## 3) Runtime Flow (request lifecycle: client → controller → service → repository → response)

The runtime wiring is “Spring outside, Clean Architecture inside”:

1. **Boot**: `SentimentScribeApplication.main(...)` calls `SpringApplication.run(...)`.
2. **Component scanning** finds controllers (`@RestController`), services (`@Service`), and configuration (`@Configuration`) under package `com.sentimentscribe`.
3. **Beans** are created:
   - Spring-managed components (`@RestController`, `@Service`).
   - Explicit beans from `com.sentimentscribe.config.AppConfig` (`@Bean` methods).
   - Configuration properties beans from `@ConfigurationProperties` records (via `@ConfigurationPropertiesScan`).
4. **HTTP request** hits Spring MVC:
   - `DispatcherServlet` routes to controller methods (`@GetMapping`, `@PostMapping`, etc.).
   - JSON request bodies are deserialized into DTO records (`com.sentimentscribe.web.dto.*`).
5. **Controller** calls a Spring service (constructor-injected).
6. **Service** constructs:
   - A use-case interactor (`...Interactor`)
   - A small presenter (an inner class implementing the use-case output boundary)
7. **Interactor** validates input and calls a **port interface** (use-case data access interface), which is implemented by a **Postgres adapter** in `com.sentimentscribe.persistence.postgres`.
8. **Presenter** captures success or failure data.
9. **Service** converts presenter state into `ServiceResult<T>`.
10. **Controller** maps `ServiceResult<T>` to `ResponseEntity`:
    - success → `200 OK` / `201 Created`
    - failure → typically `400 Bad Request` (and `500` for list failure)
11. **Spring** serializes DTOs to JSON.

Important repo-specific note: there is no `@ControllerAdvice` / global exception mapping. If an exception escapes controller/service, Spring Boot’s default 500 error handling applies.

---

## 4) Spring Concepts Used (brief explanations of annotations used in this repo and where)

Entry point:

- `@SpringBootApplication` (`SentimentScribeApplication`): enables auto-configuration, component scanning, and configuration defaults.
- `@ConfigurationPropertiesScan` (`SentimentScribeApplication`): finds and registers `@ConfigurationProperties` classes/records.

HTTP layer:

- `@RestController` (`com.sentimentscribe.web.*`): marks a class as a REST controller; return values are written as JSON.
- `@RequestMapping` (controllers): declares a URL prefix (e.g. `/api/entries`).
- `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`: route specific HTTP methods/paths.
- `@RequestBody`: JSON body → DTO record (e.g. `EntryRequest`).
- `@RequestParam`: query param → method argument (e.g. `path` in `/api/entries/by-path`).

Service layer:

- `@Service` (`com.sentimentscribe.service.*`): creates Spring-managed singleton services; constructor injection is used.

Configuration:

- `@Configuration` (`com.sentimentscribe.config.AppConfig`, `com.sentimentscribe.config.WebConfig`): configuration class.
- `@Bean` (`AppConfig`): explicit bean factory methods (used to register non-Spring classes like `StanfordCoreNLP`, DAOs).
- `@ConfigurationProperties(prefix = "sentimentscribe...")` (`AuthProperties`, `SpotifyProperties`, `TmdbProperties`, `CorsProperties`): binds keys from `application.yml` into typed records.
- `@EnableConfigurationProperties(CorsProperties.class)` (`WebConfig`): explicitly enables binding for `CorsProperties` (works alongside the global scan).

---

## 5) API Reference (list endpoints + request/response shapes + call chain)

All endpoints are under `/api/**` and implemented by `src/main/java/com/sentimentscribe/web/*Controller.java`.

### Health

- `GET /api/health`
  - Controller: `HealthController#health`
  - Response: `{ "status": "ok" }`

### Auth

- `POST /api/auth/verify`
  - Controller: `AuthController#verifyPassword(AuthRequest request)`
  - Request body: `AuthRequest` (`src/main/java/com/sentimentscribe/web/dto/AuthRequest.java`)
    - `{ "password": string }`
  - Success response: `AuthResponse` (`src/main/java/com/sentimentscribe/web/dto/AuthResponse.java`)
    - `{ "status": string, "entries": EntrySummaryResponse[] }`
  - Failure response: `ErrorResponse` (`src/main/java/com/sentimentscribe/web/dto/ErrorResponse.java`)
    - `{ "error": string }` with `400`
  - Call chain:
    - `AuthController#verifyPassword`
      → `AuthService#verifyPassword`
      → `VerifyPasswordInteractor#execute(VerifyPasswordInputData)`
      → `PostgresVerifyPasswordDataAccessObject#verifyPassword`
      → (if allowed) `PostgresDiaryEntryRepositoryAdapter#getAll`
      → controller maps `VerifyPasswordOutputData.getAllEntries()` to `EntrySummaryResponse`.

### Entries

DTOs:
- Requests: `EntryRequest` (`src/main/java/com/sentimentscribe/web/dto/EntryRequest.java`)
  - `{ "title": string, "text": string, "storagePath": string|null, "keywords": string[], "createdAt": string|null }`
- Responses:
  - `EntryResponse` (`src/main/java/com/sentimentscribe/web/dto/EntryResponse.java`)
  - `EntrySummaryResponse` (`src/main/java/com/sentimentscribe/web/dto/EntrySummaryResponse.java`)
  - `DeleteResponse` (`src/main/java/com/sentimentscribe/web/dto/DeleteResponse.java`)

Endpoints:

- `GET /api/entries`
  - Controller: `EntriesController#listEntries`
  - Success: `EntrySummaryResponse[]`
  - Failure: `ErrorResponse` with `500`
  - Call chain: `EntriesController#listEntries` → `EntryService#list` → `PostgresDiaryEntryRepositoryAdapter#getAll`

- `GET /api/entries/by-path?path=...`
  - Controller: `EntriesController#getEntryByPath(@RequestParam("path") String path)`
  - Success: `EntryResponse`
  - Failure: `ErrorResponse` with `400`
  - Call chain: `EntriesController#getEntryByPath` → `EntryService#load`
    → `LoadEntryInteractor#execute` → `PostgresDiaryEntryRepositoryAdapter#getByPath`

- `POST /api/entries`
  - Controller: `EntriesController#createEntry(@RequestBody EntryRequest request)`
  - Success: `EntryResponse` with `201`
  - Failure: `ErrorResponse` with `400`
  - Call chain: `EntriesController#createEntry` → `EntryService#save`
    → `SaveEntryInteractor#execute` → (optional keywords) `NLPKeywordExtractor#extractKeywords`
    → `PostgresDiaryEntryRepositoryAdapter#save`

- `PUT /api/entries`
  - Controller: `EntriesController#updateEntry(@RequestBody EntryRequest request)`
  - Success: `EntryResponse` with `200`
  - Failure: `ErrorResponse` with `400`
  - Call chain: same as POST; persistence identity uses `storagePath` when present.

- `DELETE /api/entries?path=...`
  - Controller: `EntriesController#deleteEntry(@RequestParam("path") String path)`
  - Success: `DeleteResponse` (`{ "deleted": true, "storagePath": "..." }`)
  - Failure: `ErrorResponse` with `400`
  - Call chain: `EntriesController#deleteEntry` → `EntryService#delete`
    → `DeleteEntryInteractor#execute` → `PostgresDiaryEntryRepositoryAdapter#deleteByPath`

### Analysis

- `POST /api/analysis`
  - Controller: `AnalysisController#analyze(AnalysisRequest request)`
  - Request: `{ "text": string }` (`AnalysisRequest`)
  - Success: `{ "keywords": string[] }` (`AnalysisResponse`)
  - Failure: `ErrorResponse` with `400`
  - Call chain: `AnalysisController#analyze` → `AnalysisService#analyze`
    → `AnalyzeKeywordsInteractor#execute` → `NLPAnalysisDataAccessObject#analyze`

### Recommendations

- `POST /api/recommendations`
  - Controller: `RecommendationsController#recommend(RecommendationRequest request)`
  - Request: `{ "text": string }` (`RecommendationRequest`)
  - Success: `RecommendationResponse`:
    - `{ "keywords": string[], "songs": SongRecommendationResponse[], "movies": MovieRecommendationResponse[] }`
  - Failure: `ErrorResponse` with `400`
  - Call chain: `RecommendationsController#recommend` → `RecommendationService#recommend`
    → `GetRecommendationsInteractor#execute`
    → `RecommendationAPIAccessObject.fetchKeywords / fetchSongRecommendations / fetchMovieRecommendations`
    → `SpotifyAPIAccessObject` + `TMDbAPIAccessObject`

---

## 6) Data/Persistence (what storage is used, key tables/files, schemas if applicable)

Storage is **PostgreSQL**, managed via Flyway migrations.

Key behavior:

- Schema lives in `src/main/resources/db/migration/V1__init.sql` and creates `users` + `diary_entries`.
- `diary_entries.storage_path` is a UNIQUE external identifier (used by the API `path` parameter).
- Keywords/analysis/recommendations are not stored; they are computed on demand.
- The database starts empty; legacy JSON files are not imported.

Repository/port structure:

- Use-case data access interfaces live under `src/main/java/com/sentimentscribe/usecase/**`.
- `src/main/java/com/sentimentscribe/data/DiaryEntryRepository.java` is a convenience interface that *extends multiple use-case ports* so Spring can inject one type into `EntryService`.
- `PostgresDiaryEntryRepositoryAdapter` implements that interface (`src/main/java/com/sentimentscribe/persistence/postgres/PostgresDiaryEntryRepositoryAdapter.java`).
- `PostgresVerifyPasswordDataAccessObject` handles password verification and default user creation (`src/main/java/com/sentimentscribe/persistence/postgres/PostgresVerifyPasswordDataAccessObject.java`).

---

## 7) How to Trace a Feature (practical debugging guide)

Start from the endpoint and follow the chain used in this repo:

1. **Controller** in `src/main/java/com/sentimentscribe/web/*Controller.java`
2. **Service** in `src/main/java/com/sentimentscribe/service/*Service.java`
3. **Interactor** in `src/main/java/com/sentimentscribe/usecase/**/**Interactor.java`
4. **Port interface** in `src/main/java/com/sentimentscribe/usecase/**/*UserDataAccessInterface.java`
5. **Adapter implementation** in `src/main/java/com/sentimentscribe/persistence/postgres/*`
6. Back out through the presenter → `ServiceResult<T>` → controller DTO mapping

Concrete example: “create entry”

- HTTP: `POST /api/entries`
- `EntriesController#createEntry` → `EntryService#save`
- `SaveEntryInteractor#execute`:
  - validates `DiaryEntry.MAX_TITLE_LENGTH`, `MIN_TEXT_LENGTH`, `MAX_TEXT_LENGTH`
  - optionally calls `NLPKeywordExtractor#extractKeywords`
  - calls persistence port `save(...)` implemented by `PostgresDiaryEntryRepositoryAdapter#save`
- controller returns `EntryResponse` with `201`

---

## 8) How to Run (dev commands)

Backend (from repo root):

- Start Postgres: `docker compose up -d postgres`
- Run API server: `mvn spring-boot:run`
- Run tests: `mvn test`

Default URLs:

- Backend: `http://localhost:8080`
- API base path: `http://localhost:8080/api`

To connect to the included React frontend, ensure the backend allows the frontend origin:

- `sentimentscribe.cors.allowed-origins` in `src/main/resources/application.yml` defaults to `http://localhost:3000`



