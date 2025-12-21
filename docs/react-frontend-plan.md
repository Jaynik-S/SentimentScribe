# React Frontend Integration Plan (SentimentScribe)

## Table of Contents

- [1. High-Level Architecture](#1-high-level-architecture)
- [2. Backend Integration Inventory (NO GUESSING)](#2-backend-integration-inventory-no-guessing)
  - [2.1 Existing REST Endpoints](#21-existing-rest-endpoints)
  - [2.2 DTO / Payload Shapes](#22-dto--payload-shapes)
  - [2.3 Backend Gaps / Clarifications Needed](#23-backend-gaps--clarifications-needed)
- [3. Interaction Mapping (Main Section)](#3-interaction-mapping-main-section)
  - [3.1 View A - Verify Password Page](#31-view-a--verify-password-page)
  - [3.2 View B - Home Menu Page](#32-view-b--home-menu-page)
  - [3.3 View C - Diary Entry Page](#33-view-c--diary-entry-page)
  - [3.4 View D - Recommendation Page](#34-view-d--recommendation-page)
- [4. Step-by-Step Implementation Plan](#4-step-by-step-implementation-plan)
  - [Step 0 - Repo Structure + React App Skeleton](#step-0--repo-structure--react-app-skeleton)
  - [Step 1 - API Client + Types](#step-1--api-client--types)
  - [Step 2 - Routing + Auth Gate + Verify Page](#step-2--routing--auth-gate--verify-page)
  - [Step 3 - Home Menu Page (List + Open/Edit + Delete Modal)](#step-3--home-menu-page-list--openedit--delete-modal)
  - [Step 4 - Diary Entry Page (Load + Keywords Toggle + Save + Recs)](#step-4--diary-entry-page-load--keywords-toggle--save--recs)
  - [Step 5 - Recommendation Page](#step-5--recommendation-page)
  - [Step 6 - Testing + UX Polish](#step-6--testing--ux-polish)
  - [Step 7 - Production Build / Deployment](#step-7--production-build--deployment)
- [5. Testing Strategy (Frontend)](#5-testing-strategy-frontend)

---

## Documentation Rules (MANDATORY)

- All implementation notes must be recorded **only** in `react-frontend-changes.md`.
- Do **not** create per-step change files (only the single `react-frontend-changes.md` file).

---

## 1. High-Level Architecture

**Goal:** Add a React SPA that consumes the existing Spring Boot REST API under `/api/**` without changing backend code.

### Runtime Pieces

- **Spring Boot backend** (existing): `http://localhost:8080`
  - REST API base path: `/api`
  - CORS is already configured via `sentimentscribe.cors.allowed-origins` (default `http://localhost:3000`) in `backend/src/main/resources/application.yml`.
- **React frontend** (new): dev server on `http://localhost:3000` (choose this port to match current backend CORS config).

### Repository Structure (Recommended)

This plan uses a monorepo layout with two independent applications:

- `/backend` — Spring Boot API (the existing backend codebase moved under this folder)
- `/frontend` — React UI application (new)

Why this structure is correct:

- Keeps Maven and Node toolchains isolated and easy to run independently.
- Makes separate deployments the default model (frontend and backend are distinct apps).
- Enforces the integration boundary: HTTP requests to `/api/**`.

How frontend and backend communicate across folders:

- Frontend reads an API base URL from env (e.g., `VITE_API_BASE_URL=http://localhost:8080`) and calls `/api/**`.
- Backend allows the frontend origin via `sentimentscribe.cors.allowed-origins` (default `http://localhost:3000`).

### Dev Setup (Recommended)

- Terminal 1 (backend): `cd backend` then `mvn spring-boot:run` (port `8080`)
- Terminal 2 (frontend): `cd frontend` then `npm run dev` (port `3000`)
- Frontend calls backend directly at `http://localhost:8080/api/...` (CORS permitted).

### Production Strategy (MANDATORY: Separate Deployments Only)

- Deploy the React frontend as its own application (static hosting/CDN).
- Deploy the Spring Boot backend as a standalone API service.
- Configure production CORS on the backend to allow the deployed frontend origin(s):
  - set `SENTIMENTSCRIBE_CORS_ORIGIN` (or `sentimentscribe.cors.allowed-origins`) to the frontend origin(s)
- Configure the frontend to target the deployed backend:
  - set `VITE_API_BASE_URL` to the backend URL in the frontend build/runtime environment

### Temporary Storage & Authentication Approach (Intentionally Minimal)

- For the current phase it is intentional and accepted to:
  - gate access to Views B–D using a client-side “unlocked” flag set after `POST /api/auth/verify`
  - display raw `storagePath` values in the UI
- Authentication/security hardening is deferred to a later phase.

### Global Loading & Error Patterns (Used Throughout All Views)

**Loading**

- Page-load calls show a blocking page overlay spinner (e.g., initial entry list load, loading an entry by path).
- Button actions show a spinner inside the triggering button and disable that button until completion.

**Errors**

- Each page has a dedicated, persistent error banner area at the top (dismissible) that renders backend `{ "error": string }` when present.
- Confirmation modals render errors inside the modal body (and keep the modal open) if the confirmed action fails.
- Field validation errors render directly under the relevant input and prevent sending invalid requests.

---

## 2. Backend Integration Inventory (NO GUESSING)

### 2.1 Existing REST Endpoints

All endpoints below were found in `backend/src/main/java/com/sentimentscribe/web/*Controller.java`.

| Controller | Method name | HTTP | URL | Success | Failure |
|---|---|---:|---|---|---|
| `AuthController` | `verifyPassword(AuthRequest)` | POST | `/api/auth/verify` | `200` → `AuthResponse` | `400` → `ErrorResponse` |
| `EntriesController` | `listEntries()` | GET | `/api/entries` | `200` → `EntrySummaryResponse[]` | `500` → `ErrorResponse` |
| `EntriesController` | `getEntryByPath(path)` | GET | `/api/entries/by-path?path=...` | `200` → `EntryResponse` | `400` → `ErrorResponse` |
| `EntriesController` | `createEntry(EntryRequest)` | POST | `/api/entries` | `201` → `EntryResponse` | `400` → `ErrorResponse` |
| `EntriesController` | `updateEntry(EntryRequest)` | PUT | `/api/entries` | `200` → `EntryResponse` | `400` → `ErrorResponse` |
| `EntriesController` | `deleteEntry(path)` | DELETE | `/api/entries?path=...` | `200` → `DeleteResponse` | `400` → `ErrorResponse` |
| `AnalysisController` | `analyze(AnalysisRequest)` | POST | `/api/analysis` | `200` → `AnalysisResponse` | `400` → `ErrorResponse` |
| `RecommendationsController` | `recommend(RecommendationRequest)` | POST | `/api/recommendations` | `200` → `RecommendationResponse` | `400` → `ErrorResponse` |
| `HealthController` | `health()` | GET | `/api/health` | `200` → `{ "status": "ok" }` | (n/a) |

### 2.2 DTO / Payload Shapes

These are the exact Java records used by the controllers under `backend/src/main/java/com/sentimentscribe/web/dto/*`.

**Requests**

- `POST /api/auth/verify` body (`AuthRequest`)
  - `{ "password": string }`
- `POST /api/analysis` body (`AnalysisRequest`)
  - `{ "text": string }`
- `POST /api/recommendations` body (`RecommendationRequest`)
  - `{ "text": string }`
- `POST /api/entries` body (`EntryRequest`)
- `PUT /api/entries` body (`EntryRequest`)
  - `{ "title": string, "text": string, "storagePath": string|null, "keywords": string[], "createdAt": LocalDateTime|null }`
  - Note: `storagePath` is the persistence identifier (file path). For new entries, send `null`.

**Responses**

- Common error response (`ErrorResponse`)
  - `{ "error": string }`
- Auth success (`AuthResponse`)
  - `{ "status": string, "entries": EntrySummaryResponse[] }`
  - Known status strings (from `VerifyPasswordDataAccessObject`):
    - `"Correct Password"`
    - `"Created new password."`
    - Incorrect password does **not** return `200` (it returns `400` with `{ "error": "Incorrect Password" }`)
- Entry list item (`EntrySummaryResponse`)
  - `{ "title": string, "storagePath": string, "createdAt": LocalDateTime|null, "updatedAt": LocalDateTime|null, "keywords": string[] }`
- Entry load/save (`EntryResponse`)
  - `{ "title": string, "text": string, "storagePath": string, "createdAt": LocalDateTime|null, "updatedAt": LocalDateTime|null, "keywords": string[] }`
- Delete success (`DeleteResponse`)
  - `{ "deleted": boolean, "storagePath": string }`
- Keywords analysis (`AnalysisResponse`)
  - `{ "keywords": string[] }`
- Recommendations (`RecommendationResponse`)
  - `{ "keywords": string[], "songs": SongRecommendationResponse[], "movies": MovieRecommendationResponse[] }`
- Song recommendation (`SongRecommendationResponse`)
  - `{ "releaseYear": string, "imageUrl": string, "songName": string, "artistName": string, "popularityScore": string, "externalUrl": string }`
- Movie recommendation (`MovieRecommendationResponse`)
  - `{ "releaseYear": string, "imageUrl": string, "movieTitle": string, "movieRating": string, "overview": string }`

**Date/time note (Frontend needs to be consistent)**

- `createdAt` / `updatedAt` are `java.time.LocalDateTime` on the backend.
- Frontend should send/parse them as strings in `YYYY-MM-DDTHH:mm:ss` (optionally with fractional seconds), **without timezone suffix**.
  - ⚠️ Requires backend clarification: confirm the accepted JSON format for `LocalDateTime` in this project’s Jackson config (no explicit config is present in `application.yml`).

### 2.3 Backend Gaps / Clarifications Needed

These are **not** requests to change the backend in this plan—just items the frontend must not assume incorrectly.

- `EntryResponse.updatedAt` is currently always `null` from `EntriesController` (the controller passes `null` explicitly). The UI should rely on `EntrySummaryResponse.updatedAt` from `GET /api/entries` when showing “date edited”.
- ⚠️ Requires backend clarification: `"Created new password."` persistence across restarts (likely runtime-only unless `SENTIMENTSCRIBE_PASSWORD` is set externally).
- Recommendations require valid external API keys (Spotify/TMDb). When keys are missing/invalid, the backend may return `400` with `ErrorResponse`; the frontend keeps the user on the current page and renders the message in the page’s error banner.

---

## 3. Interaction Mapping (Main Section)

**Global frontend conventions used in the mapping below**

- API base URL (dev): `http://localhost:8080`
- All requests/JSON use `Content-Type: application/json`
- Loading handling:
  - page-load API calls show the global blocking overlay spinner until completion
  - button-click API calls show an in-button spinner and disable the triggering button until completion
- Error handling:
  - for non-2xx responses, parse `{ "error": string }` when possible and render it in the page’s error banner region
  - form errors also render inline under the relevant input when applicable
- Route guarding (temporary, intentional):
  - after successful `POST /api/auth/verify`, set `auth.isUnlocked = true` and persist `sessionStorage["sentimentscribe.isUnlocked"] = "true"`
  - on app start, restore `auth.isUnlocked` from `sessionStorage`
  - if locked, redirect attempts to access Views B–D back to View A

### 3.1 View A — Verify Password Page

**UI elements**

- Password input (type: `password`)
- Button: `Submit / Enter`
- Page error banner region (dismissible; shows backend error text)
- Inline error under the password input (shows backend error text on failed verify)
- Loading behavior:
  - disable password input + submit button while verifying
  - show in-button spinner on `Submit / Enter` while verifying

**Interaction mapping**

- View: Verify Password
- Button: `Submit / Enter`
- Trigger condition:
  - click the button OR press Enter while focused in password input
- HTTP method + URL:
  - `POST http://localhost:8080/api/auth/verify`
- Request payload (JSON):
  - `{ "password": <passwordInputValue> }`
  - Source of fields:
    - `password` ← value from password input
- Response payload:
  - Success (`200`): `AuthResponse`
    - `status` (string)
    - `entries` (array of `EntrySummaryResponse`)
  - Failure (`400`): `ErrorResponse`
    - `error` (string), commonly `"Incorrect Password"`
- Success criteria:
  - HTTP status `200`
- UI updates on success:
  - Clear any existing page error banner + inline password error
  - Set client auth state:
    - `auth.isUnlocked = true`
    - `auth.status = response.status`
    - `sessionStorage["sentimentscribe.isUnlocked"] = "true"`
  - Clear the password input value
- Navigation:
  - Success → navigate to View B (Home Menu)
  - Failure → stay on View A; render `ErrorResponse.error` in both the page error banner and the inline password error; clear the password input and focus it

### 3.2 View B — Home Menu Page

**UI elements**

- Page error banner region (dismissible; includes action buttons like `Retry`)
- Table of entries (rows are clickable for open/edit) with columns:
  - `Title` ← `EntrySummaryResponse.title`
    - Under the title text, display `EntrySummaryResponse.storagePath` (monospace, smaller text; intentional/temporary)
  - `Date Created` ← `EntrySummaryResponse.createdAt` (formatted for display)
  - `Date Edited` ← `EntrySummaryResponse.updatedAt` (formatted; may be `null`)
  - `Keywords` ← `EntrySummaryResponse.keywords` (render as comma-separated text or chips)
  - `Delete` ← per-row delete button (must not trigger row-click navigation)
- Button: `New Entry`
- Delete confirmation modal (required):
  - Shows the entry `title` and `storagePath`
  - Buttons: `Cancel`, `Confirm Delete`
  - Inline modal error area (renders backend `ErrorResponse.error` on delete failure)
- Loading behavior:
  - On initial list load, show the global blocking overlay spinner
  - On delete confirm, show an in-button spinner on `Confirm Delete` and disable modal buttons

**Interaction mapping**

- View: Home Menu
- Trigger: Page load / route entry
- HTTP method + URL:
  - `GET http://localhost:8080/api/entries`
- Request payload:
  - none
- Response payload:
  - Success (`200`): `EntrySummaryResponse[]`
  - Failure (`500`): `ErrorResponse`
- Success criteria:
  - HTTP status `200`
- UI updates on success:
  - Populate table rows from response array
- Failure behavior:
  - Hide the loading overlay
  - Render `ErrorResponse.error` in the page error banner with a `Retry` button that re-runs `GET /api/entries`
  - Keep the table visible but empty (no partial stale data)
- Navigation:
  - None (stays on View B)

---

- View: Home Menu
- Trigger: Row click (primary open/edit action)
- Trigger condition:
  - click anywhere on a row **except** interactive controls (e.g., the Delete button)
- Backend call:
  - none in View B (loading happens on View C)
- Data passed to next view:
  - `storagePath` ← the row’s `EntrySummaryResponse.storagePath`
  - Pass as a URL query param: navigate to View C at `/entry?path=<urlEncodedStoragePath>`
- Navigation:
  - navigate to View C (Diary Entry) in edit mode

---

- View: Home Menu
- Button: `Delete` (per row)
- Trigger condition:
  - click the row’s Delete button (must not trigger row navigation; stop event propagation)
- UI updates (modal behavior):
  - Open the delete confirmation modal (no backend call yet)
  - Modal buttons:
    - `Cancel` closes the modal with no changes
    - `Confirm Delete` triggers the backend delete call (below)

---

- View: Home Menu
- Button: `Confirm Delete` (inside modal)
- Trigger condition:
  - click `Confirm Delete` in the modal
- HTTP method + URL:
  - `DELETE http://localhost:8080/api/entries?path=<storagePath>`
  - Query param:
    - `path` ← the selected row’s `EntrySummaryResponse.storagePath` (URL-encoded)
- Request payload:
  - none (query param only)
- Response payload:
  - Success (`200`): `DeleteResponse` → `{ "deleted": true, "storagePath": string }`
  - Failure (`400`): `ErrorResponse` → `{ "error": string }`
- Success criteria:
  - HTTP status `200` AND `deleted === true`
- UI updates on success:
  - Close the modal
  - Show a non-blocking success toast/banner: `Deleted entry`
  - Refresh the table by calling `GET /api/entries` again (shows loading overlay during the refresh)
- UI updates on failure:
  - Keep the modal open
  - Render `ErrorResponse.error` inside the modal error area
  - Re-enable `Confirm Delete` so the user can retry or cancel
- Navigation:
  - Stay on View B

---

- View: Home Menu
- Button: `New Entry`
- Trigger condition:
  - click the button
- Backend call:
  - none
- UI updates:
  - Initialize an in-memory “entry draft” state:
    - `title = ""`
    - `text = ""`
    - `keywords = []`
    - `keywordsVisible = false` (dropdown hidden initially)
    - `storagePath = null`
    - `createdAt = now()` formatted as `LocalDateTime` string (see View C)
- Navigation:
  - navigate to View C (Diary Entry)

### 3.3 View C — Diary Entry Page

**UI elements**

- Page error banner region (dismissible; can include actions like `Back to Home`)
- Title input
- Keywords dropdown/list panel (directly under the title input):
  - Hidden when `keywordsVisible = false`
  - Visible when `keywordsVisible = true`
  - Renders keywords from `entry.keywords` (from `AnalysisResponse.keywords` or saved entry response)
- Text body textarea
- Metadata display:
  - `Created At` ← `entry.createdAt` (new entries: generated by frontend; existing entries: loaded from backend)
  - `Storage Path` ← `entry.storagePath` (displayed intentionally/temporarily when present)
- Buttons:
  - Keywords toggle button (label depends on UI state):
    - if `keywordsVisible = false` → label `Show Keywords`
    - if `keywordsVisible = true` → label `Hide Keywords`
  - `Save Entry`
  - `Get Media Recommendations`
- Loading behavior:
  - If opening an existing entry (via row click), show global blocking overlay while loading the entry
  - For each button action, show an in-button spinner + disable that button while its request is in flight

**Field validation (match backend constraints from `SaveEntryInteractor` / `DiaryEntry`)**

- Title:
  - required, `1..30` characters
- Text:
  - required, `50..1000` characters

**Date generation / formatting (important)**

- New entry flow: generate `createdAt` on the frontend when the user clicks `New Entry` (View B) and carry it through saves.
- Existing entry flow: load `createdAt` from `GET /api/entries/by-path` and preserve it on subsequent updates.
- Send `createdAt` to the backend using a timezone-less string format compatible with `LocalDateTime`, e.g. `2025-12-20T14:23:10` (do not append `Z` unless confirmed acceptable).

**Interaction mapping**

- View: Diary Entry
- Trigger: Page load in edit mode (when URL has `?path=...`)
- HTTP method + URL:
  - `GET http://localhost:8080/api/entries/by-path?path=<storagePath>`
- Request payload:
  - Query param:
    - `path` ← URL query param value from View B row click (`EntrySummaryResponse.storagePath`)
- Response payload:
  - Success (`200`): `EntryResponse`
    - `title`, `text`, `storagePath`, `createdAt`, `updatedAt`, `keywords`
  - Failure (`400`): `ErrorResponse`
- Success criteria:
  - HTTP status `200`
- UI updates on success:
  - Populate the form state from `EntryResponse`:
    - `entry.title = response.title`
    - `entry.text = response.text`
    - `entry.storagePath = response.storagePath`
    - `entry.createdAt = response.createdAt` (if `null`, keep existing state value)
    - `entry.keywords = response.keywords`
  - Set `keywordsVisible = false` initially (keywords are loaded but hidden until the user toggles them)
  - Clear any page errors and hide the loading overlay
- UI updates on failure:
  - Hide the loading overlay
  - Render `ErrorResponse.error` in the page error banner with a `Back to Home` action (navigates to View B)
- Navigation:
  - Success → stay on View C
  - Failure → user chooses `Back to Home` or remains on View C

---

- View: Diary Entry
- Button: Keywords toggle (`Show Keywords` / `Hide Keywords`)
- Trigger condition:
  - click the toggle button
- Branch A — label is `Show Keywords` (currently hidden):
  - HTTP method + URL:
    - `POST http://localhost:8080/api/analysis`
  - Request payload (JSON):
    - `{ "text": <analysisText> }`
    - `analysisText` ← `title + "\\n\\n" + text`
  - Response payload:
    - Success (`200`): `AnalysisResponse` → `{ "keywords": string[] }`
    - Failure (`400`): `ErrorResponse` → `{ "error": string }`
  - Success criteria:
    - HTTP status `200`
  - UI state transitions on success:
    - set `entry.keywords = response.keywords`
    - set `keywordsVisible = true` (dropdown/list becomes visible directly under the title)
    - button label changes to `Hide Keywords`
  - UI state transitions on failure:
    - keep `keywordsVisible = false`
    - render `ErrorResponse.error` in the page error banner
    - button label remains `Show Keywords`
- Branch B — label is `Hide Keywords` (currently visible):
  - Backend call:
    - none
  - UI state transitions:
    - set `keywordsVisible = false` (dropdown/list is hidden)
    - button label changes to `Show Keywords`

---

- View: Diary Entry
- Button: `Save Entry`
- Trigger condition:
  - click the button
- HTTP method + URL (depends on whether this is create vs update):
  - If `entry.storagePath` is `null`/empty → **Create**
    - `POST http://localhost:8080/api/entries`
  - Else → **Update**
    - `PUT http://localhost:8080/api/entries`
- Request payload (JSON) (`EntryRequest`):
  - `{ "title": <title>, "text": <text>, "storagePath": <storagePathOrNull>, "keywords": <keywords>, "createdAt": <createdAt> }`
  - Field sources:
    - `title` ← title input
    - `text` ← text body input
    - `storagePath` ← current entry state
    - `keywords` ← `entry.keywords` (may be empty; backend may auto-extract)
    - `createdAt` ← current entry state (generated for new entries; preserved for existing entries)
- Response payload:
  - Create success (`201`): `EntryResponse`
  - Update success (`200`): `EntryResponse`
  - Failure (`400`): `ErrorResponse`
- Success criteria:
  - Create: HTTP `201`
  - Update: HTTP `200`
- UI updates on success:
  - Update state from response:
    - always set `entry.storagePath = response.storagePath`
    - set `entry.createdAt = response.createdAt` if present
    - set `entry.keywords = response.keywords` if present
  - Show a non-blocking success toast/banner: `Saved`
- UI updates on failure:
  - Render `ErrorResponse.error` in the page error banner
  - If the error clearly maps to a field (e.g., starts with `Title` or `Text`), also render it inline under that field
- Navigation:
  - Success → stay on View C
  - Failure → stay on View C

---

- View: Diary Entry
- Button: `Get Media Recommendations`
- Trigger condition:
  - click the button
- HTTP method + URL:
  - `POST http://localhost:8080/api/recommendations`
- Request payload (JSON):
  - `{ "text": <recommendationText> }`
  - `recommendationText` ← `title + "\\n\\n" + text`
- Response payload:
  - Success (`200`): `RecommendationResponse`
    - `keywords: string[]`
    - `songs: SongRecommendationResponse[]`
    - `movies: MovieRecommendationResponse[]`
  - Failure (`400`): `ErrorResponse`
- Success criteria:
  - HTTP status `200`
- UI updates on success:
  - Store the `RecommendationResponse` in in-memory recommendations state used by View D
- UI updates on failure:
  - Render `ErrorResponse.error` in the page error banner
- Navigation:
  - Success → navigate to View D (Recommendation Page)
  - Failure → stay on View C

### 3.4 View D — Recommendation Page

**UI elements**

- Table/grid with two columns/sections:
  - **Songs** list/table
  - **Movies** list/table
- Render all available metadata fields from backend response:
  - Songs: `songName`, `artistName`, `releaseYear`, `popularityScore`, `externalUrl`, `imageUrl`
  - Movies: `movieTitle`, `movieRating`, `releaseYear`, `overview`, `imageUrl`
- Button: `Back`
- Empty-state messaging if lists are empty
- Error state if recommendations are missing in client state (e.g., user reloads page)

**Interaction mapping**

- View: Recommendation
- Trigger: Page load / route entry
- Backend call:
  - none (display the stored `RecommendationResponse` captured from `POST /api/recommendations`)
  - If the page is loaded without stored state (e.g., user refreshes the browser):
    - render the page error banner message: `Recommendations not available. Return to the entry and click Get Media Recommendations again.`
    - keep the `Back` button visible so the user can return to View C

---

- View: Recommendation
- Button: `Back`
- Trigger condition:
  - click the button
- Backend call:
  - none
- UI updates:
  - keep the current entry draft state intact (title/text/keywords/createdAt/storagePath) so the user returns to what they were editing
- Navigation:
  - navigate to View C (Diary Entry)

---

## 4. Step-by-Step Implementation Plan

### Step 0 - Repo Structure + React App Skeleton

**Goal:** Adopt the `/backend` + `/frontend` structure and scaffold the React SPA with predictable dev ports.

**Planned files/components**

- Move existing Spring Boot backend into `backend/`:
  - `backend/pom.xml`
  - `backend/src/main/...`
  - `backend/src/test/...`
- Create the React frontend in `frontend/`:
  - React app scaffold via Vite (suggested): `frontend/package.json`, `frontend/vite.config.ts`, `frontend/src/*`
- Minimal styling setup (plain CSS or a UI library decided here)

**Backend endpoints used/validated**

- `GET /api/health` (smoke check connectivity)

**Checklist**

- [ ] Create `backend/` and move the existing backend project files into it (no code changes; only relocation)
- [ ] Create `frontend/` React project (Vite React + TypeScript recommended)
- [ ] Configure the frontend dev server to run on `http://localhost:3000` (matches backend CORS default)
- [ ] Add `frontend/.env.development` with `VITE_API_BASE_URL=http://localhost:8080`
- [ ] Document how to run locally:
  - `cd backend; mvn spring-boot:run`
  - `cd frontend; npm run dev`

---

### Step 1 — API Client + Types

**Goal:** Centralize API calls with exact request/response shapes and consistent error handling.

**Planned files/components**

- `frontend/src/api/types.ts` (TypeScript types mirroring backend DTOs)
- `frontend/src/api/http.ts` (fetch wrapper: base URL, JSON, error parsing)
- `frontend/src/api/auth.ts`
- `frontend/src/api/entries.ts`
- `frontend/src/api/analysis.ts`
- `frontend/src/api/recommendations.ts`

**Backend endpoints used/validated**

- `POST /api/auth/verify`
- `GET /api/entries`
- `DELETE /api/entries?path=...`
- `POST /api/analysis`
- `POST /api/recommendations`
- `POST /api/entries`, `PUT /api/entries`, `GET /api/entries/by-path?path=...` (used in later steps, but types should exist now)

**Checklist**

- [ ] Implement one error shape: always surface `{ error: string }` for non-2xx responses
- [ ] Ensure query param encoding for `path` (storagePath)
- [ ] Add a date helper: convert JS `Date` to `LocalDateTime` string without timezone suffix
- [ ] Add basic “API smoke” calls in a temporary debug component or console (remove once UI exists)

---

### Step 2 - Routing + Auth Gate + Verify Page

**Goal:** Implement View A and route-guard Views B-D behind "unlock".

**Planned files/components**

- `frontend/src/routes.tsx` (React Router routes)
- `frontend/src/state/auth.tsx` (Auth context/store: `isUnlocked`, `status`)
- `frontend/src/components/PageErrorBanner.tsx` (global page error banner region)
- `frontend/src/components/GlobalLoadingOverlay.tsx` (blocking overlay spinner for page-load calls)
- `frontend/src/pages/VerifyPasswordPage.tsx` (View A)
- `frontend/src/components/ToastHost.tsx` (optional: consistent success toasts like `Saved` / `Deleted entry`)

**Backend endpoints used/validated**

- `POST /api/auth/verify`
- (optional smoke) `GET /api/health`

**Checklist**

- [ ] Create routes for Views A-D (`/`, `/home`, `/entry`, `/recommendations`)
- [ ] Implement auth gate: redirect to `/` if `!isUnlocked` (restore `isUnlocked` from `sessionStorage["sentimentscribe.isUnlocked"]` on app start)
- [ ] Add global layout that always renders:
  - [ ] `PageErrorBanner` region (dismissible)
  - [ ] `GlobalLoadingOverlay` (visible when a page-load request is active)
- [ ] Verify page submit calls `POST /api/auth/verify` with `{ password }`
- [ ] On `200`, set auth state + persist `sessionStorage["sentimentscribe.isUnlocked"]="true"` + navigate to `/home`
- [ ] On `400`, render backend `{ error }` inline under the password input and in the page error banner; remain on `/`

---

### Step 3 - Home Menu Page (List + Open/Edit + Delete Modal)

**Goal:** Implement View B with entry list rendering and delete behavior.

**Planned files/components**

- `frontend/src/pages/HomeMenuPage.tsx` (View B)
- `frontend/src/components/EntriesTable.tsx`
- `frontend/src/components/DeleteEntryModal.tsx` (confirmation modal)

**Backend endpoints used/validated**

- `GET /api/entries`
- `DELETE /api/entries?path=...`

**Checklist**

- [ ] On page load, call `GET /api/entries` and render rows
- [ ] Render columns: title (including visible `storagePath` under title), createdAt, updatedAt, keywords, delete button
- [ ] Implement row click as the primary open/edit action:
  - [ ] clicking a row navigates to `/entry?path=<urlEncodedStoragePath>`
  - [ ] clicking delete does not trigger row navigation (stop propagation)
- [ ] Implement delete confirmation modal:
  - [ ] Delete button opens modal; no backend call yet
  - [ ] Confirm Delete calls `DELETE /api/entries?path=<storagePath>`
  - [ ] On success: close modal, show `Deleted entry` toast, then re-fetch `GET /api/entries`
  - [ ] On failure: keep modal open and show backend `{ error }` inside the modal
- [ ] New Entry button navigates to `/entry` and initializes a new draft entry state (including `keywordsVisible=false`)

---

### Step 4 - Diary Entry Page (Load + Keywords Toggle + Save + Recs)

**Goal:** Implement View C including keyword analysis, save (create/update), and recommendation request.

**Planned files/components**

- `frontend/src/state/entryDraft.tsx` (draft store: title/text/keywords/keywordsVisible/storagePath/createdAt)
- `frontend/src/pages/DiaryEntryPage.tsx` (View C)
- `frontend/src/components/KeywordsDropdown.tsx` (renders keyword list directly under title when visible)

**Backend endpoints used/validated**

- `GET /api/entries/by-path?path=...`
- `POST /api/analysis`
- `POST /api/entries`
- `PUT /api/entries`
- `POST /api/recommendations`

**Checklist**

- [ ] Support edit mode via query param:
  - [ ] If route is `/entry?path=...`, call `GET /api/entries/by-path?path=<storagePath>` on page load
  - [ ] On success, populate draft state from `EntryResponse` and start with `keywordsVisible=false`
  - [ ] On failure, show page error banner with `Back to Home` action
- [ ] Title + text inputs update draft state
- [ ] Keywords toggle behavior:
  - [ ] If hidden, clicking `Show Keywords` calls `POST /api/analysis` with `{ "text": title + "\\n\\n" + text }`, then sets `keywordsVisible=true`
  - [ ] If visible, clicking `Hide Keywords` hides the dropdown/list with no backend call
- [ ] Save Entry:
  - [ ] If no `storagePath`, `POST /api/entries`
  - [ ] Else, `PUT /api/entries`
  - [ ] Always send `createdAt` and preserve it across saves
- [ ] Get Media Recommendations calls `POST /api/recommendations` and navigates to `/recommendations` on success
- [ ] Implement validation that matches backend constraints to reduce avoidable `400`s

---

### Step 5 — Recommendation Page

**Goal:** Implement View D display for songs/movies and back navigation to the diary entry draft.

**Planned files/components**

- `frontend/src/pages/RecommendationPage.tsx` (View D)
- `frontend/src/state/recommendations.tsx` (store last RecommendationResponse; optional)
- `frontend/src/components/SongsTable.tsx`
- `frontend/src/components/MoviesTable.tsx`

**Backend endpoints used/validated**

- None at render-time (data comes from Step 4 call to `POST /api/recommendations`)

**Checklist**

- [ ] Render song metadata: name, artist, year, popularity, externalUrl, image
- [ ] Render movie metadata: title, rating, year, overview, image
- [ ] Handle empty lists with friendly empty state
- [ ] Back button returns to `/entry` with draft intact
- [ ] Handle refresh-without-state: render the page error banner message `Recommendations not available...` and keep `Back` available (no refetch)

---

### Step 6 — Testing + UX Polish

**Goal:** Add basic automated tests and UX quality improvements (loading, errors, validation).

**Planned files/components**

- Component tests for the 4 pages
- API client tests (mock fetch)
- Shared UI components (error banner, loading spinner, toast)

**Backend endpoints used/validated**

- No new endpoints; verify the complete flow end-to-end manually against real backend.

**Checklist**

- [ ] Unit tests: verify correct request payloads are formed for each button
- [ ] Unit tests: verify non-2xx responses surface `{ error }`
- [ ] Manual smoke checklist across the 4 views (see Testing Strategy)
- [ ] Add disable states to prevent double submits

---

### Step 7 - Production Build / Deployment

**Goal:** Document the required separate deployments setup and production CORS/env configuration.

**Backend endpoints used/validated**

- Same REST endpoints; validate from the deployed frontend domain.

**Checklist**

- [ ] Configure backend CORS for the deployed frontend origin(s) via `SENTIMENTSCRIBE_CORS_ORIGIN` (or `sentimentscribe.cors.allowed-origins`)
- [ ] Configure frontend prod `VITE_API_BASE_URL` to point at the deployed backend
- [ ] Document build/run commands and a release checklist

---

## 5. Testing Strategy (Frontend)

### Unit Tests (Component-level)

- Verify Password page
  - Submitting calls `POST /api/auth/verify` with `{ password }`
  - `200` persists unlock + navigates to Home; `400` renders backend `{ error }` in the page banner + inline and stays on Verify
- Home Menu page
  - On mount, calls `GET /api/entries`
  - Row click navigates to `/entry?path=<storagePath>` (Diary Entry loads via `GET /api/entries/by-path`)
  - Delete opens confirmation modal; confirm calls `DELETE /api/entries?path=<storagePath>`; cancel does not call backend
  - On successful delete, re-fetches `GET /api/entries`; on failure, shows backend `{ error }` inside the modal
- Diary Entry page
  - If opened with `?path=...`, calls `GET /api/entries/by-path?path=...` on mount and populates fields
  - `Show Keywords` calls `POST /api/analysis` with `{ text }`, then shows the dropdown under title and toggles label to `Hide Keywords`
  - `Hide Keywords` hides the dropdown with no backend call
  - Save Entry chooses POST vs PUT based on `storagePath` and sends the full `EntryRequest`
  - Get Media Recommendations calls `POST /api/recommendations` and navigates on success
- Recommendation page
  - Renders songs/movies fields correctly and handles empty lists
  - On refresh-without-state, renders the page error banner and keeps `Back` available

### Integration-level Checks (API client)

- One test per endpoint function verifying:
  - URL + method
  - request body and query params
  - error parsing to `{ error: string }`

### Manual Smoke Test Checklist (Tied to Views/Buttons)

- View A: enter wrong password → see error banner + inline password error; enter correct password → navigates to Home
- View B: table loads; click a row → opens that entry in Diary Entry; click delete → modal appears; confirm → entry removed after refresh
- View C: show/hide keywords toggles dropdown under title; save shows toast; get media recommendations navigates to Recommendations
- View D: songs/movies render; refresh page shows “Recommendations not available…” banner; Back returns to Diary Entry with draft preserved
