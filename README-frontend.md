# SentimentScribe Frontend (React) — Repo-Specific Guide

Audience: you know programming basics, but are new to React. This document explains how *this repository’s* React frontend works end-to-end and how it connects to the Spring Boot backend.

---

## 1) Project Overview (what the frontend does + user flow)

This frontend is a React + TypeScript single-page app (SPA) built with Vite. It implements the diary UI by calling the backend REST API under `/api/**` and rendering the returned JSON:

1. Verify password (unlock gate)
2. Home menu (list entries + delete)
3. Diary entry editor (load/create/update + analyze keywords)
4. Recommendations (songs + movies)

Repo-specific architecture:

- Routing: `react-router-dom` data router in `frontend/src/routes.tsx`
- Global state: React Context providers in `frontend/src/state/*`
- Backend calls: `frontend/src/api/*` built on `frontend/src/api/http.ts#request`
- Global UX: top error banner + full-page loading overlay rendered by the root layout

---

## 2) Directory Walkthrough (what each folder/file group is for)

- `frontend/src/`: application source code
  - Entry points: `frontend/src/main.tsx`, `frontend/src/App.tsx`, `frontend/src/routes.tsx`
  - Pages: `frontend/src/pages/*`
  - Components: `frontend/src/components/*`
  - State (Context): `frontend/src/state/*`
  - API client: `frontend/src/api/*`
  - Tests: `frontend/src/**/__tests__/*` + `frontend/src/test/*`
- `frontend/public/`: static assets copied as-is (e.g. `vite.svg` favicon)
- `frontend/vite.config.ts`: dev server config (port `3000`, no proxy)
- `frontend/.env.development`: dev backend base URL (`VITE_API_BASE_URL=http://localhost:8080`)
- `frontend/dist/`, `frontend/node_modules/`, `frontend/.vite/`: generated outputs (not authored app code)

---

## 3) Routing & Navigation (all routes and what components render)

Routes are defined in `frontend/src/routes.tsx`:

- `/` → `VerifyPasswordPage` (index route)
- `/home` → `HomeMenuPage` (protected by `RequireUnlocked`)
- `/entry` → `DiaryEntryPage` (protected)
  - Optional query param: `?path=<storagePath>` to load an existing entry
- `/recommendations` → `RecommendationPage` (protected)

Navigation is done with `useNavigate()` in pages:

- `VerifyPasswordPage` → `/home` on successful verify
- `HomeMenuPage` → `/entry?path=...` on row click; `/entry` for “New Entry”
- `DiaryEntryPage` → `/home` (Back) and `/recommendations` after fetching recs
- `RecommendationPage` → `/entry` (Back)

How data is passed between pages in this repo:

- Auth gate: `useAuth().isUnlocked` (persisted in `sessionStorage`) controls access to protected routes.
- Entry selection: query param `path` is read via `useSearchParams()` in `DiaryEntryPage`.
- Draft persistence: `EntryDraftProvider` stores the draft so navigating away/back keeps inputs.
- Recommendations persistence: `RecommendationsProvider` stores the last recommendation response in memory only (refreshing `/recommendations` loses it).

---

## 4) State Management (where state lives + keywords toggle + loading/error pattern)

All global state is implemented with React Context providers (wired in `frontend/src/App.tsx`):

- `frontend/src/state/auth.tsx`
  - Stores `isUnlocked` (sessionStorage-backed) and `status` from backend auth response.
  - Used by `RequireUnlocked` in `frontend/src/routes.tsx`.
- `frontend/src/state/entryDraft.tsx`
  - Stores the current draft: `title`, `text`, `storagePath`, `keywords`, `createdAt`.
  - Stores `keywordsVisible` (controls whether keyword chips render).
  - Provides `startNewEntry()` which resets the draft and sets `createdAt` using `formatLocalDateTime(new Date())`.
- `frontend/src/state/recommendations.tsx`
  - Stores `recommendations: RecommendationResponse | null` so `/recommendations` can render without refetching.
- `frontend/src/state/ui.tsx`
  - Stores `pageError` + optional `pageErrorAction` for `PageErrorBanner`.
  - Stores `isPageLoading` for `GlobalLoadingOverlay`.

Keywords toggle behavior (exact implementation):

- In `frontend/src/pages/DiaryEntryPage.tsx#handleKeywordsToggle`:
  - If keywords are visible: clicking “Hide Keywords” just sets `keywordsVisible=false` (no API call).
  - If hidden: clicking “Show Keywords” calls `POST /api/analysis` with `text: title + "\n\n" + text`, saves returned keywords into draft, then sets `keywordsVisible=true`.

Loading + error pattern (repo-specific):

- Global loading overlay is used for “page-load” operations:
  - `HomeMenuPage` around `GET /api/entries`
  - `DiaryEntryPage` when loading entry by query param (`GET /api/entries/by-path`)
- Button actions use local “in-flight” state to disable buttons and show spinners:
  - Verify: `isSubmitting`
  - Delete modal: `isDeleting`
  - Entry page: `isAnalyzing`, `isSaving`, `isRecommending`
- Backend errors are parsed into `ApiError` by `frontend/src/api/http.ts` and displayed as:
  - Global banner: `frontend/src/components/PageErrorBanner.tsx`
  - Inline errors (password, title/text) and modal errors (delete)

---

## 5) API Client Layer (exact files; base URL; proxy/CORS; request/response shapes)

### Files responsible for backend calls

- `frontend/src/api/http.ts`
  - Base URL resolution (`VITE_API_BASE_URL`)
  - `request<T>()` wrapper for `fetch()`
  - `ApiError` + `isApiError()` for consistent error handling
- Endpoint modules:
  - `frontend/src/api/auth.ts` (`POST /api/auth/verify`)
  - `frontend/src/api/entries.ts` (`GET/POST/PUT/DELETE /api/entries...`)
  - `frontend/src/api/analysis.ts` (`POST /api/analysis`)
  - `frontend/src/api/recommendations.ts` (`POST /api/recommendations`)
- Shared DTO types:
  - `frontend/src/api/types.ts` mirrors backend DTO shapes in `src/main/java/com/sentimentscribe/web/dto/*`

### Base URL and dev setup

- `frontend/.env.development` sets `VITE_API_BASE_URL=http://localhost:8080`.
- `frontend/src/api/http.ts` reads `import.meta.env.VITE_API_BASE_URL` and falls back to `http://localhost:8080`.

### CORS/proxy handling in this repo

There is no Vite proxy configured (see `frontend/vite.config.ts`). In dev, the browser calls:

- frontend: `http://localhost:3000`
- backend: `http://localhost:8080/api/...`

So the backend must allow CORS for the frontend origin. In this repo, backend CORS is configured by:

- `src/main/resources/application.yml` → `sentimentscribe.cors.allowed-origins`
- `src/main/java/com/sentimentscribe/config/WebConfig.java#addCorsMappings`

### Backend calls used by the UI (repo-specific inventory)

All shapes below come from `frontend/src/api/types.ts` and are used by pages/components.

- Verify password
  - Function: `frontend/src/api/auth.ts#verifyPassword`
  - `POST /api/auth/verify`
  - Request: `{ password: string }`
  - Response: `{ status: string, entries: EntrySummaryResponse[] }`
  - Used by: `VerifyPasswordPage` (sets unlocked flag + navigates; does not use `entries`)

- List entries
  - Function: `frontend/src/api/entries.ts#listEntries`
  - `GET /api/entries`
  - Response: `EntrySummaryResponse[]`
  - Used by: `HomeMenuPage` + `EntriesTable`

- Load entry
  - Function: `frontend/src/api/entries.ts#getEntryByPath`
  - `GET /api/entries/by-path?path=...`
  - Response: `EntryResponse`
  - Used by: `DiaryEntryPage` when `?path=...` is present

- Create entry
  - Function: `frontend/src/api/entries.ts#createEntry`
  - `POST /api/entries`
  - Request: `EntryRequest`
  - Response: `EntryResponse`
  - Used by: `DiaryEntryPage` when `draft.storagePath` is null

- Update entry
  - Function: `frontend/src/api/entries.ts#updateEntry`
  - `PUT /api/entries`
  - Request: `EntryRequest`
  - Response: `EntryResponse`
  - Used by: `DiaryEntryPage` when `draft.storagePath` is set

- Delete entry
  - Function: `frontend/src/api/entries.ts#deleteEntry`
  - `DELETE /api/entries?path=...`
  - Response: `{ deleted: boolean, storagePath: string }`
  - Used by: `HomeMenuPage` delete modal flow

- Analyze keywords
  - Function: `frontend/src/api/analysis.ts#analyzeText`
  - `POST /api/analysis`
  - Request: `{ text: string }` (frontend sends `title + "\n\n" + text`)
  - Response: `{ keywords: string[] }`
  - Used by: `DiaryEntryPage` keywords toggle

- Get recommendations
  - Function: `frontend/src/api/recommendations.ts#getRecommendations`
  - `POST /api/recommendations`
  - Request: `{ text: string }` (frontend sends `title + "\n\n" + text`)
  - Response: `{ keywords: string[], songs: SongRecommendationResponse[], movies: MovieRecommendationResponse[] }`
  - Used by: `DiaryEntryPage` then rendered by `RecommendationPage`

---

## 6) Page-by-Page Breakdown (Verify Password, Home Menu table, Diary Entry, Recommendations)

### Verify Password — `frontend/src/pages/VerifyPasswordPage.tsx`

- Handler: `handleSubmit`
  - Validates non-empty password
  - Calls `verifyPassword({ password })`
  - On success: `useAuth().setUnlocked(true, response.status)` then `navigate('/home')`
  - On failure: uses `isApiError(error)` to show backend `{ error }` message in both inline error and global banner

### Home Menu — `frontend/src/pages/HomeMenuPage.tsx`

- On mount: `loadEntries()` calls `listEntries()` and shows global loading overlay (`useUi().setPageLoading`)
- Renders `EntriesTable`:
  - row click navigates to `/entry?path=<encoded storagePath>`
  - delete click opens `DeleteEntryModal` (with `event.stopPropagation()` to avoid row navigation)
- Delete confirm:
  - calls `deleteEntry(storagePath)`
  - on success: reloads the list by calling `loadEntries()` again

### Diary Entry — `frontend/src/pages/DiaryEntryPage.tsx`

- Load existing entry:
  - if `useSearchParams().get('path')` exists, calls `getEntryByPath(path)` and `setDraft(...)`
- Keywords toggle:
  - “Show Keywords” → `analyzeText({ text: title + "\n\n" + text })` → saves keywords into draft → shows chips
  - “Hide Keywords” just hides chips (no request)
- Save entry:
  - Validates title/text length to match backend constraints (30/50/1000)
  - Ensures `createdAt` exists (`formatLocalDateTime(new Date())`)
  - POST vs PUT is chosen by `draft.storagePath`
- Recommendations:
  - Calls `getRecommendations({ text: title + "\n\n" + text })`
  - Stores response in `RecommendationsProvider` then navigates to `/recommendations`

### Recommendations — `frontend/src/pages/RecommendationPage.tsx`

- Reads `useRecommendations().recommendations`
- If null (common on refresh): sets global banner explaining how to recover
- Renders `SongsTable` and `MoviesTable` with `recommendations?.songs/movies ?? []`

---

## 7) End-to-End Traces (3 flows: verify password, open/edit entry, get recommendations)

### A) Verify password

`VerifyPasswordPage#handleSubmit`
→ `api/auth.ts#verifyPassword`
→ `api/http.ts#request`
→ backend `POST /api/auth/verify`
→ `state/auth.tsx#setUnlocked(true)`
→ `navigate('/home')`

### B) Open an entry from the home table

`HomeMenuPage` mount
→ `api/entries.ts#listEntries` (`GET /api/entries`)
→ click row in `EntriesTable`
→ `navigate('/entry?path=...')`
→ `DiaryEntryPage` load effect sees `path`
→ `api/entries.ts#getEntryByPath` (`GET /api/entries/by-path?path=...`)
→ `state/entryDraft.tsx#setDraft(...)`

### C) Get recommendations

Click “Get Media Recommendations” in `DiaryEntryPage#handleRecommendations`
→ `api/recommendations.ts#getRecommendations` (`POST /api/recommendations`)
→ `state/recommendations.tsx#setRecommendations(...)`
→ `navigate('/recommendations')`
→ `RecommendationPage` renders `SongsTable` + `MoviesTable`

---

## 8) Tests/Lint/Build (how to run, what failures mean)

From `frontend/`:

- Install deps: `npm install`
- Dev: `npm run dev`
- Build: `npm run build`
- Lint: `npm run lint`
- Tests: `npm run test` (runs `vitest run`)

Repo-specific test structure:

- API tests: `frontend/src/api/__tests__/*`
  - mock `fetch` via `frontend/src/test/mockFetch.ts`
  - assert URL/method/body/query encoding (e.g. `entries.test.ts`)
- Page tests: `frontend/src/pages/__tests__/*`
  - mock API modules with `vi.mock(...)`
  - render with providers + router via `frontend/src/test/renderWithRouter.tsx`

---

## 9) How to Debug/Extend (where to add fields, trace a button click, common pitfalls)

If a button “does nothing”:

1. Check the local in-flight guard in the page (`isSubmitting`, `isSaving`, `isRecommending`, etc.).
2. Check the global error banner (`frontend/src/components/PageErrorBanner.tsx`) for the actual message.
3. In dev, use `window.sentimentScribeApi` (registered by `frontend/src/api/debug.ts` only when `import.meta.env.DEV`) to call `listEntries()`, etc.

To trace a UI action to the backend:

Page handler → `frontend/src/api/*.ts` function → `frontend/src/api/http.ts#request` → backend controller endpoint under `/api/**`.

To add a new field to an entry (touchpoints in this repo):

- Frontend types: `frontend/src/api/types.ts` (`EntryRequest`, `EntryResponse`, `EntrySummaryResponse`)
- Draft state: `frontend/src/state/entryDraft.tsx#EntryDraft`
- Entry editor payload: `frontend/src/pages/DiaryEntryPage.tsx` (builds `payload: EntryRequest`)
- Home display: `frontend/src/components/EntriesTable.tsx` (if the field should appear there)
- Backend DTOs and mapping:
  - `src/main/java/com/sentimentscribe/web/dto/EntryRequest.java`
  - `src/main/java/com/sentimentscribe/web/dto/EntryResponse.java`
  - `src/main/java/com/sentimentscribe/web/dto/EntrySummaryResponse.java`
  - `src/main/java/com/sentimentscribe/web/EntriesController.java`

Repo-specific pitfalls:

- Refreshing `/recommendations` loses state because recommendations are not persisted (in-memory context only).
- Adding a new page that should be gated requires wrapping it in `RequireUnlocked` in `frontend/src/routes.tsx`.
- Avoid bypassing `request()`; it centralizes base URL + `{ error }` parsing.

---

## 10) How to Run (dev commands)

Frontend (from `frontend/`):

- `npm install`
- `npm run dev` (Vite on `http://localhost:3000`)

Backend (from repo root):

- `mvn spring-boot:run` (API on `http://localhost:8080`)

Integration checklist:

- `frontend/.env.development` points to the backend (`VITE_API_BASE_URL`).
- Backend CORS allows `http://localhost:3000` (see backend `application.yml` + `WebConfig`).

