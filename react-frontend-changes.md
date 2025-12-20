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

## Step 1 — API Client + Types

### Summary of changes

_TODO_

### Files modified/created

_TODO_

### Per-file notes (what changed + why + backend/API connection)

_TODO_

### How to verify (commands + manual checks)

_TODO_

---

## Step 2 — Routing + Auth Gate + Verify Page

### Summary of changes

_TODO_

### Files modified/created

_TODO_

### Per-file notes (what changed + why + backend/API connection)

_TODO_

### How to verify (commands + manual checks)

_TODO_

---

## Step 3 — Home Menu Page (List + Open/Edit + Delete Modal)

### Summary of changes

_TODO_

### Files modified/created

_TODO_

### Per-file notes (what changed + why + backend/API connection)

_TODO_

### How to verify (commands + manual checks)

_TODO_

---

## Step 4 — Diary Entry Page (Load + Keywords Toggle + Save + Recs)

### Summary of changes

_TODO_

### Files modified/created

_TODO_

### Per-file notes (what changed + why + backend/API connection)

_TODO_

### How to verify (commands + manual checks)

_TODO_

---

## Step 5 — Recommendation Page

### Summary of changes

_TODO_

### Files modified/created

_TODO_

### Per-file notes (what changed + why + backend/API connection)

_TODO_

### How to verify (commands + manual checks)

_TODO_

---

## Step 6 — Testing + UX Polish

### Summary of changes

_TODO_

### Files modified/created

_TODO_

### Per-file notes (what changed + why + backend/API connection)

_TODO_

### How to verify (commands + manual checks)

_TODO_

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
