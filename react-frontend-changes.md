# React Frontend Changes Log (Template)

> Rule: Record **all** implementation notes in this file only (no per-step change files).

## Step 0 - Repo Structure + React App Skeleton

### Summary of changes

Moved the Spring Boot backend into `backend/` and scaffolded a Vite React + TypeScript app in `frontend/`. Configured the frontend dev server for port `3000`, added the dev API base URL env file, and documented local run commands.

### Files modified/created

- Moved: `pom.xml` -> `backend/pom.xml`
- Moved: `src/` -> `backend/src/`
- Moved: `target/` -> `backend/target/`
- Added: `frontend/` (Vite React + TypeScript scaffold)
- Added: `frontend/.env.development`
- Updated: `frontend/vite.config.ts`
- Updated: `README.md`

### Per-file notes (what changed + why + backend/API connection)

- `backend/pom.xml`, `backend/src/`, `backend/target/`: backend project relocated to match `/backend` monorepo layout.
- `frontend/vite.config.ts`: set dev server port to `3000` to align with backend CORS defaults.
- `frontend/.env.development`: sets `VITE_API_BASE_URL=http://localhost:8080` for local API calls.
- `README.md`: added local run commands for `backend` and `frontend` apps.

### How to verify (commands + manual checks)

- `cd backend; mvn spring-boot:run`
- `cd frontend; npm install; npm run dev`

---

## Step 1 - API Client + Types

### Summary of changes

Added TypeScript DTOs, a fetch wrapper with consistent error handling, and per-endpoint API helpers. Included a LocalDateTime formatter and a temporary debug console hook for manual smoke calls.

### Files modified/created

- Added: `frontend/src/api/types.ts`
- Added: `frontend/src/api/http.ts`
- Added: `frontend/src/api/localDateTime.ts`
- Added: `frontend/src/api/auth.ts`
- Added: `frontend/src/api/entries.ts`
- Added: `frontend/src/api/analysis.ts`
- Added: `frontend/src/api/recommendations.ts`
- Added: `frontend/src/api/debug.ts`
- Updated: `frontend/src/main.tsx`

### Per-file notes (what changed + why + backend/API connection)

- `frontend/src/api/types.ts`: mirrors backend DTOs to keep request/response shapes aligned with `/api/**`.
- `frontend/src/api/http.ts`: central fetch helper that enforces JSON parsing and throws an `ApiError` with `{ error: string }` for non-2xx responses.
- `frontend/src/api/localDateTime.ts`: formats JS `Date` into `LocalDateTime` strings without timezone suffixes.
- `frontend/src/api/auth.ts`: `POST /api/auth/verify`.
- `frontend/src/api/entries.ts`: `GET /api/entries`, `GET /api/entries/by-path`, `POST /api/entries`, `PUT /api/entries`, `DELETE /api/entries?path=...` with URL-encoded `path`.
- `frontend/src/api/analysis.ts`: `POST /api/analysis`.
- `frontend/src/api/recommendations.ts`: `POST /api/recommendations`.
- `frontend/src/api/debug.ts`: exposes `window.moodverseApi` helpers for manual smoke calls during Step 1.
- `frontend/src/main.tsx`: registers the dev-only debug helpers on startup.

### How to verify (commands + manual checks)

- Run the frontend and call `window.moodverseApi.smoke()` in devtools to hit `GET /api/entries`.
- Optional: `window.moodverseApi.verifyPassword('<password>')` to hit `POST /api/auth/verify`.

---

## Step 2 - Routing + Auth Gate + Verify Page

### Summary of changes

Added React Router routing with a global layout, auth guard, and a Verify Password page wired to `POST /api/auth/verify`. Introduced auth/UI state providers plus shared error/loading components, and refreshed base styles to support the new layout.

### Files modified/created

- Added: `frontend/src/routes.tsx`
- Added: `frontend/src/state/auth.tsx`
- Added: `frontend/src/state/ui.tsx`
- Added: `frontend/src/components/PageErrorBanner.tsx`
- Added: `frontend/src/components/GlobalLoadingOverlay.tsx`
- Added: `frontend/src/pages/VerifyPasswordPage.tsx`
- Added: `frontend/src/pages/HomeMenuPage.tsx`
- Added: `frontend/src/pages/DiaryEntryPage.tsx`
- Added: `frontend/src/pages/RecommendationPage.tsx`
- Updated: `frontend/src/App.tsx`
- Updated: `frontend/src/App.css`
- Updated: `frontend/src/index.css`
- Updated: `frontend/package.json`

### Per-file notes (what changed + why + backend/API connection)

- `frontend/src/routes.tsx`: defines routes for `/`, `/home`, `/entry`, `/recommendations`, with an auth guard redirecting locked users back to `/`.
- `frontend/src/state/auth.tsx`: stores `isUnlocked` and auth status, backed by `sessionStorage["moodverse.isUnlocked"]`.
- `frontend/src/state/ui.tsx`: global page error and loading state for the error banner and loading overlay.
- `frontend/src/components/PageErrorBanner.tsx`: dismissible banner for `{ error }` responses.
- `frontend/src/components/GlobalLoadingOverlay.tsx`: blocking overlay for page-load requests.
- `frontend/src/pages/VerifyPasswordPage.tsx`: implements View A, calling `POST /api/auth/verify` and navigating to `/home` on success.
- `frontend/src/pages/HomeMenuPage.tsx`: placeholder for View B.
- `frontend/src/pages/DiaryEntryPage.tsx`: placeholder for View C.
- `frontend/src/pages/RecommendationPage.tsx`: placeholder for View D.
- `frontend/src/App.tsx`: wraps the app with auth/UI providers and router.
- `frontend/src/App.css` / `frontend/src/index.css`: base styles for layout, banner, and verify page.
- `frontend/package.json`: adds `react-router-dom`.

### How to verify (commands + manual checks)

- `cd frontend; npm install; npm run dev`
- Visit `/` and submit a password to see `POST /api/auth/verify` and auth redirect to `/home`.

---

## Step 3 - Home Menu Page (List + Open/Edit + Delete Modal)

### Summary of changes

Implemented the Home Menu page with entry listing, row navigation, delete confirmation modal, and a simple success toast. Added entry draft state initialization for the New Entry flow, plus retry-capable error handling for list failures.

### Files modified/created

- Added: `frontend/src/components/EntriesTable.tsx`
- Added: `frontend/src/components/DeleteEntryModal.tsx`
- Added: `frontend/src/state/entryDraft.tsx`
- Updated: `frontend/src/pages/HomeMenuPage.tsx`
- Updated: `frontend/src/state/ui.tsx`
- Updated: `frontend/src/components/PageErrorBanner.tsx`
- Updated: `frontend/src/App.tsx`
- Updated: `frontend/src/App.css`

### Per-file notes (what changed + why + backend/API connection)

- `frontend/src/pages/HomeMenuPage.tsx`: calls `GET /api/entries` on load, routes row clicks to `/entry?path=...`, triggers `DELETE /api/entries?path=...` via modal, and shows `Deleted entry` toast on success.
- `frontend/src/components/EntriesTable.tsx`: renders entry list with title, storage path, created/edited dates, keywords, and delete button.
- `frontend/src/components/DeleteEntryModal.tsx`: confirmation modal showing backend error messages on delete failure.
- `frontend/src/state/entryDraft.tsx`: provides `startNewEntry()` to initialize a new draft (with `keywordsVisible=false`) before navigating to `/entry`.
- `frontend/src/state/ui.tsx` and `frontend/src/components/PageErrorBanner.tsx`: allow an action button (Retry) when list loading fails.
- `frontend/src/App.tsx` / `frontend/src/App.css`: wires the draft provider and styles the Home Menu layout, table, modal, and toast.

### How to verify (commands + manual checks)

- `cd frontend; npm install; npm run dev`
- Visit `/home` after unlocking; confirm list loads, row click navigates to `/entry?path=...`, delete modal works, and retry loads entries after a forced error.

---

## Step 4 - Diary Entry Page (Load + Keywords Toggle + Save + Recs)

### Summary of changes

Built the Diary Entry page with edit-mode loading, keyword analysis toggling, save (create/update) logic, and recommendation requests that store the response for View D. Added inline validation, metadata display, and supporting UI styles.

### Files modified/created

- Added: `frontend/src/components/KeywordsDropdown.tsx`
- Added: `frontend/src/state/recommendations.tsx`
- Updated: `frontend/src/pages/DiaryEntryPage.tsx`
- Updated: `frontend/src/state/entryDraft.tsx`
- Updated: `frontend/src/App.tsx`
- Updated: `frontend/src/App.css`

### Per-file notes (what changed + why + backend/API connection)

- `frontend/src/pages/DiaryEntryPage.tsx`: loads entries via `GET /api/entries/by-path`, toggles keywords via `POST /api/analysis`, saves via `POST/PUT /api/entries`, and fetches recommendations via `POST /api/recommendations`.
- `frontend/src/components/KeywordsDropdown.tsx`: renders keywords list under the title input when visible.
- `frontend/src/state/recommendations.tsx`: stores the last `RecommendationResponse` for View D.
- `frontend/src/state/entryDraft.tsx`: exports draft type for keyword UI and continues to manage draft state.
- `frontend/src/App.tsx`: wires in the recommendations provider.
- `frontend/src/App.css`: styles the diary entry form, metadata panel, keywords, and textarea.

### How to verify (commands + manual checks)

- `cd frontend; npm install; npm run dev`
- Create/edit an entry: verify load by path, keyword toggle, save (POST vs PUT), and recommendations request navigation.

---

## Step 5 - Recommendation Page

### Summary of changes

Implemented the Recommendation page to render stored song/movie results, handle empty lists, and show a banner error when the page is refreshed without state.

### Files modified/created

- Added: `frontend/src/components/SongsTable.tsx`
- Added: `frontend/src/components/MoviesTable.tsx`
- Updated: `frontend/src/pages/RecommendationPage.tsx`
- Updated: `frontend/src/App.css`

### Per-file notes (what changed + why + backend/API connection)

- `frontend/src/pages/RecommendationPage.tsx`: reads the in-memory `RecommendationResponse` and renders songs/movies; shows the required banner message if state is missing.
- `frontend/src/components/SongsTable.tsx`: renders song metadata fields from `RecommendationResponse.songs`.
- `frontend/src/components/MoviesTable.tsx`: renders movie metadata fields from `RecommendationResponse.movies`.
- `frontend/src/App.css`: adds layout styles for the recommendations sections/cards and empty states.

### How to verify (commands + manual checks)

- `cd frontend; npm install; npm run dev`
- Navigate to `/recommendations` after requesting recs and verify song/movie fields render; refresh the page to see the missing-state banner.

---

## Step 6 - Testing + UX Polish

### Summary of changes

Added a Vitest-based test setup with API client and page-level component tests covering the four views, plus helper utilities for router/provider rendering.

### Files modified/created

- Added: `frontend/src/test/setup.ts`
- Added: `frontend/src/test/renderWithRouter.tsx`
- Added: `frontend/src/test/mockFetch.ts`
- Added: `frontend/src/api/__tests__/http.test.ts`
- Added: `frontend/src/api/__tests__/auth.test.ts`
- Added: `frontend/src/api/__tests__/analysis.test.ts`
- Added: `frontend/src/api/__tests__/entries.test.ts`
- Added: `frontend/src/api/__tests__/recommendations.test.ts`
- Added: `frontend/src/pages/__tests__/VerifyPasswordPage.test.tsx`
- Added: `frontend/src/pages/__tests__/HomeMenuPage.test.tsx`
- Added: `frontend/src/pages/__tests__/DiaryEntryPage.test.tsx`
- Added: `frontend/src/pages/__tests__/RecommendationPage.test.tsx`
- Updated: `frontend/package.json`
- Updated: `frontend/vite.config.ts`

### Per-file notes (what changed + why + backend/API connection)

- `frontend/src/test/*`: test setup, router/provider rendering helper, and fetch mocks for API tests.
- `frontend/src/api/__tests__/*`: validates API client URLs, methods, payloads, and error parsing.
- `frontend/src/pages/__tests__/*`: verifies primary actions for each page (verify, list/delete, save/analyze/recommend, and recommendations rendering).
- `frontend/package.json`: adds test script and dev dependencies for Vitest and Testing Library.
- `frontend/vite.config.ts`: configures Vitest to run with jsdom and setup file.

### How to verify (commands + manual checks)

- `cd frontend; npm install; npm run test`
- Optional: `npm run test:run` for CI-style runs.

---

## Step 7 — Production Build / Deployment

### Summary of changes

_TODO_

### Files modified/created

_TODO_

### Per-file notes (what changed + why + backend/API connection)

_TODO_

### How to verify (commands + manual checks)

_TODO_
