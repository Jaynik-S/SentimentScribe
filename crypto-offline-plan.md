# Crypto + Offline Implementation Plan (SentimentScribe)

> Planning-only document. Do **not** implement directly from this file without also updating `crypto-offline-changes.md` as you go.
>
> Change documentation rule (MANDATORY): after completing **each** step in Section 9, document what you did under the matching section in `crypto-offline-changes.md`:
> - files modified/created/deleted
> - what changed in each file
> - how it connects to the architecture/data flow
> - verification performed

## Scope (this plan covers only)

- (3) On-device encryption in the React app (Web Crypto API)
- (4) Offline-first support using IndexedDB (sync encrypted entries when online)

## Explicit non-goals (out of scope)

- Multi-device “true sync” (pull remote changes + merge conflict UI). This plan can **bootstrap** from the server when local cache is empty, but does not attempt multi-device reconciliation.
- Offline login/registration (requires backend reachability). Offline behavior applies after you already have a valid `sentimentscribe.auth` session.

## Assumptions (call out if false before implementing)

- Backend entry persistence already stores ciphertext-only fields (`titleCiphertext`, `titleIv`, `bodyCiphertext`, `bodyIv`, `algo`, `version`) and returns `createdAt`/`updatedAt`.
- Frontend already derives an in-memory AES key from a user passphrase using PBKDF2 params delivered by the backend.
- For offline-first creates, we can safely generate a unique `storagePath` client-side (see Step 9). Backend accepts a client-provided `storagePath` and treats it as the identity (confirmed by `PostgresDiaryEntryRepositoryAdapter#save`).

---

## Table of Contents

- [0) Current State Inventory (repo-specific)](#0-current-state-inventory-repo-specific)
- [1) Target Data Flow Overview (most important)](#1-target-data-flow-overview-most-important)
- [2) Encrypted Payload Specification (versioned)](#2-encrypted-payload-specification-versioned)
- [3) Crypto Module Design (Web Crypto API)](#3-crypto-module-design-web-crypto-api)
- [4) Lock/Unlock UX (separate from backend auth)](#4-lockunlock-ux-separate-from-backend-auth)
- [5) Entry Editor Flow Updates](#5-entry-editor-flow-updates)
- [6) IndexedDB Design + Sync Queue](#6-indexeddb-design--sync-queue)
- [7) Sync Engine (online detection + pushpull)](#7-sync-engine-online-detection--pushpull)
- [8) Offline-Resilient UI](#8-offline-resilient-ui)
- [9) Step-by-Step Implementation Plan (execution ready)](#9-step-by-step-implementation-plan-execution-ready)
- [10) Risks, Gotchas, and Debugging Guide](#10-risks-gotchas-and-debugging-guide)

---

## 0) Current State Inventory (repo-specific)

### 0.1 Where entries are loaded/saved today (frontend)

- Home list load:
  - `frontend/src/pages/HomeMenuPage.tsx` calls `listEntries()` on mount.
  - It decrypts titles client-side with `decryptAesGcm(...)` if an E2EE key is present.
  - On API failure it shows a global error banner; it does **not** fall back to local storage.
- Entry load:
  - `frontend/src/pages/DiaryEntryPage.tsx` reads `?path=<storagePath>` and calls `getEntryByPath(path)`.
  - It decrypts `{titleCiphertext,titleIv,bodyCiphertext,bodyIv,algo,version}` via `decryptEnvelope(...)`.
- Entry save:
  - `frontend/src/pages/DiaryEntryPage.tsx#handleSave`:
    - validates plaintext lengths locally
    - encrypts with `encryptEnvelope(title, body, key)`
    - sends ciphertext-only `EntryRequest` via `createEntry(...)` or `updateEntry(...)`
- Entry delete:
  - `frontend/src/pages/HomeMenuPage.tsx` calls `deleteEntry(storagePath)`, then reloads via `listEntries()`.

### 0.2 API client functions used

- Central fetch wrapper: `frontend/src/api/http.ts#request`
  - Attaches `Authorization: Bearer <token>` from `frontend/src/state/auth.tsx#getAccessToken`.
  - On `401`, clears stored auth (`clearStoredAuth()`) and redirects to `/`.
- Entries API: `frontend/src/api/entries.ts`
  - `listEntries(): GET /api/entries`
  - `getEntryByPath(path): GET /api/entries/by-path?path=...`
  - `createEntry(payload): POST /api/entries`
  - `updateEntry(payload): PUT /api/entries`
  - `deleteEntry(path): DELETE /api/entries?path=...`

### 0.3 Entry DTO shapes (frontend types)

Defined in `frontend/src/api/types.ts`:

- `EntrySummaryResponse` (home list):
  - `storagePath`, `createdAt`, `updatedAt`
  - `titleCiphertext`, `titleIv`
  - `algo`, `version`
- `EntryRequest` / `EntryResponse` (entry editor):
  - `storagePath`, `createdAt` (request), `updatedAt` (response)
  - `titleCiphertext`, `titleIv`, `bodyCiphertext`, `bodyIv`
  - `algo`, `version`

### 0.4 Auth token + E2EE parameter handling (frontend)

- Auth session storage: `frontend/src/state/auth.tsx`
  - `sessionStorage['sentimentscribe.auth']` holds `{ accessToken, user, e2eeParams }`
- Unlock flow:
  - Route gate: `frontend/src/routes.tsx#RequireUnlocked` checks `useE2ee().isUnlocked`
  - Page: `frontend/src/pages/UnlockPage.tsx` prompts for passphrase and calls `useE2ee().unlock(passphrase, auth.e2eeParams)`
- Key derivation & storage:
  - `frontend/src/state/e2ee.tsx` stores `CryptoKey | null` **in memory only**.
  - Derivation: `frontend/src/crypto/kdf.ts#deriveAesKey` uses PBKDF2 (salt/iterations from backend).
  - Encryption helpers: `frontend/src/crypto/aesGcm.ts` (AES-GCM with random 12-byte IV).
  - Envelope: `frontend/src/crypto/envelope.ts` (`ENVELOPE_VERSION = 1`, `ENVELOPE_ALGO = 'AES-GCM'`).

### 0.5 Current state management approach

- Global state: React Context providers in `frontend/src/state/*` and wired in `frontend/src/App.tsx`.
  - `AuthProvider`, `E2eeProvider`, `EntryDraftProvider`, `RecommendationsProvider`, `UiProvider`.
- Per-page local UI state:
  - Home uses local `useState` for entries list, delete modal state, toast.
  - Entry page uses local `useState` for validation errors and in-flight flags.

---

## 1) Target Data Flow Overview (most important)

### 1.1 BEFORE (today)

**Online save (today)**

```
DiaryEntryPage (plaintext in React state)
  → encryptEnvelope(title, body, key)
  → POST/PUT /api/entries (ciphertext fields)
  → backend stores ciphertext in Postgres
```

**Online load (today)**

```
HomeMenuPage
  → GET /api/entries (title ciphertext only)
  → decryptAesGcm(titleCiphertext, titleIv, key)
  → render titlePlaintext

DiaryEntryPage
  → GET /api/entries/by-path?path=...
  → decryptEnvelope(...)
  → render plaintext editor
```

**Offline behavior (today)**

```
API unreachable
  → Home shows error banner and empty list
  → Entry load fails (error banner)
  → Save/delete fail (error banner)
```

### 1.2 AFTER (target)

Key principle: **ciphertext is the persistence format**. Plaintext exists only in memory (React state) and is transformed at the edges:
- Encrypt **before** any persistence (backend or IndexedDB).
- Decrypt **after** loading from a persistence source into UI state.

**Online save (target)**

```
UI plaintext
  → encrypt (AES-GCM per field, random IV per entry/field)
  → write ciphertext record to IndexedDB.entries (immediate local persistence)
  → enqueue syncQueue operation (upsert)
  → attempt sync (push ciphertext to POST/PUT /api/entries)
  → on success: update IndexedDB record with server updatedAt, clear dirty flag, dequeue
```

**Online load (target)**

```
Home: IndexedDB.entries (ciphertext) → decrypt title → UI
Entry: IndexedDB.entries (ciphertext) → decrypt title/body → UI

(Optional bootstrap) if IndexedDB empty and API reachable:
  → fetch from /api/entries and /api/entries/by-path as needed
  → write ciphertext to IndexedDB (no plaintext ever stored)
```

**Offline save (target)**

```
UI plaintext
  → encrypt
  → write ciphertext to IndexedDB.entries
  → enqueue syncQueue operation (upsert)
  → UI updates immediately from IndexedDB (source of truth while offline)
```

**Offline load (target)**

```
IndexedDB.entries ciphertext
  → decrypt
  → UI plaintext
```

**Sync (target)**

```
online detected (navigator.onLine + events + optional /api/health)
  → flush syncQueue in order
    - upsert: PUT /api/entries (ciphertext-only)
    - delete: DELETE /api/entries?path=...
  → minimal retry/backoff
```

---

## 2) Encrypted Payload Specification (versioned)

### 2.1 Canonical encrypted envelope (frontend)

Use a **versioned envelope** that matches existing code in `frontend/src/crypto/envelope.ts` (this is the payload format we persist locally and then map onto backend DTO fields).

**EncryptedEntryEnvelopeV1**

```ts
{
  version: 1,
  algo: "AES-GCM",

  // Title
  titleCiphertext: "<base64>",
  titleIv: "<base64>",

  // Body
  bodyCiphertext: "<base64>",
  bodyIv: "<base64>"
}
```

Encoding rules:
- `ciphertext` is base64 of raw AES-GCM output bytes.
- `iv` is base64 of 12 random bytes (`crypto.getRandomValues(new Uint8Array(12))`).
- Plaintext is UTF-8 encoded with `TextEncoder` before encryption; decrypted bytes are decoded with `TextDecoder`.

### 2.2 What is persisted where

**Persisted to backend entry endpoints (`/api/entries**`)**

- `storagePath` (string): external identifier used throughout the app and API.
- `createdAt` (string / LocalDateTime): for creates; backend sets `updatedAt` on save.
- Envelope fields (ciphertext only): `titleCiphertext`, `titleIv`, `bodyCiphertext`, `bodyIv`, `algo`, `version`.

**Local-only (IndexedDB only)**

- `userId` (to partition local cache per authenticated user)
- Sync metadata:
  - `dirty` (boolean)
  - `deletedAt` (nullable timestamp) OR `isDeleted` (boolean)
  - `lastAttemptAt`, `retryCount`, `lastError` (for queue items)

### 2.3 Mapping to existing backend DTOs/endpoints (do not invent)

Backend already expects the same flattened fields (no nested JSON) via:
- Request: `backend/src/main/java/com/sentimentscribe/web/dto/EntryRequest.java`
- Response: `backend/src/main/java/com/sentimentscribe/web/dto/EntryResponse.java`

Mapping:

- `EntryRequest.titleCiphertext` ← `envelope.titleCiphertext`
- `EntryRequest.titleIv` ← `envelope.titleIv`
- `EntryRequest.bodyCiphertext` ← `envelope.bodyCiphertext`
- `EntryRequest.bodyIv` ← `envelope.bodyIv`
- `EntryRequest.algo` ← `envelope.algo`
- `EntryRequest.version` ← `envelope.version`

No plaintext fields exist in these DTOs today; keep it that way.

---

## 3) Crypto Module Design (Web Crypto API)

### 3.1 Where to put it (repo conventions)

Crypto code already lives under `frontend/src/crypto/*`. Keep that folder as the single source of crypto primitives.

Add a small “public” crypto module that matches the required shape **deriveKey → encrypt → decrypt** without forcing pages to know file-level details:

- Proposed new module: `frontend/src/crypto/diaryCrypto.ts`
  - re-exports or wraps existing `kdf.ts`, `aesGcm.ts`, `envelope.ts`
  - provides the stable API used by UI and offline modules

### 3.2 Functions to expose (required)

- `deriveKey(passphrase, { salt, iterations, hash? }): Promise<CryptoKey>`
  - Wraps `deriveAesKey(...)` and validates `kdf === 'PBKDF2-SHA256'` from `E2eeParams`.
- `encrypt(plaintext, key): Promise<{ version, algo, iv, ciphertext }>`
  - For single-field usage (optional, but useful for titles in list pages).
- `decrypt(envelope, key): Promise<string>`
  - For single-field usage.

And keep the entry-level helpers (already exist as `encryptEnvelope` / `decryptEnvelope`):
- `encryptEntry({ title, body }, key): Promise<EncryptedEntryEnvelopeV1>`
- `decryptEntry(encryptedEntryEnvelope, key): Promise<{ title, body }>`

### 3.3 AES-GCM details (required)

- Algorithm: `AES-GCM`
- Key size: 256-bit
- IV/nonce: 12 bytes random per encryption call
- Auth tag: default (Web Crypto returns ciphertext+tag as one ArrayBuffer)

### 3.4 PBKDF2 params + salt storage strategy

- KDF: PBKDF2 with SHA-256
- Iterations: provided by backend per user (`auth.e2eeParams.iterations`, currently 310,000)
- Salt: provided by backend per user (`auth.e2eeParams.salt`, base64 of 16 bytes)
- Storage: store **only** salt/iterations/kdf in `sessionStorage` inside `sentimentscribe.auth` (already done).

Do not store:
- raw passphrase
- derived `CryptoKey` (keep in memory only while unlocked)

### 3.5 Key clearing strategy (best-effort)

You cannot reliably “zero” a `CryptoKey`, but you can:
- Remove references (`setKey(null)`)
- Stop all decrypt/encrypt operations when locked
- On lock/logout/tab close, clear key + sensitive UI state (draft plaintext) where feasible

---

## 4) Lock/Unlock UX (separate from backend auth)

### 4.1 When unlock is required

Current routing already enforces this:
- `/home`, `/entry`, `/recommendations` are wrapped in `RequireUnlocked` in `frontend/src/routes.tsx`.

Keep this behavior. It ensures:
- Offline access still requires the local passphrase (good).
- Plaintext editor is never reachable while locked.

### 4.2 Lock behavior (add explicit lock)

Add a “Lock” action that:
- clears `useE2ee().key`
- clears any plaintext draft state (at least the current `EntryDraftProvider` draft)
- navigates to `/unlock`

Where to surface it (minimal):
- Add a secondary button in the header of:
  - `frontend/src/pages/HomeMenuPage.tsx`
  - `frontend/src/pages/DiaryEntryPage.tsx`

### 4.3 Logout behavior (best-effort)

There is no explicit logout UI today. Still ensure:
- Any call to `useAuth().setAuth(null)` or `useAuth().clear()` clears the derived key (already true via `frontend/src/state/e2ee.tsx` effect).
- Automatic 401 handling in `frontend/src/api/http.ts` clears stored auth; E2EE key should also clear once auth becomes null.

### 4.4 Tab close / refresh

- Refresh already drops the in-memory key; user must unlock again.
- Add best-effort clear on tab close:
  - `window.addEventListener('beforeunload', ...)` inside `E2eeProvider` to call `clear()`.

---

## 5) Entry Editor Flow Updates

### 5.1 Create entry (target)

**Flow**

```
Save button
  → validate plaintext
  → encrypt entry envelope (title/body)
  → persist ciphertext to IndexedDB.entries (immediate)
  → enqueue syncQueue upsert
  → attempt sync if online
  → update UI (toast + metadata)
```

**Key requirement**
- Plaintext must never touch backend entry persistence endpoints (`/api/entries`).

**Repo-specific “button → function → crypto → persistence” mapping**
- Click **Save Entry** button in `frontend/src/pages/DiaryEntryPage.tsx`
  - Handler: `DiaryEntryPage#handleSave`
  - Crypto step: `encryptEnvelope(draft.title, draft.text, key)` (via the Step 1 wrapper module)
  - Persistence steps (target):
    - write ciphertext to `IndexedDB.entries` via `frontend/src/offline/entriesRepo.ts`
    - enqueue sync via `frontend/src/offline/syncQueueRepo.ts`
    - attempt push via `frontend/src/offline/syncEngine.ts` which calls `frontend/src/api/entries.ts` (`PUT/POST /api/entries`)
  - UI/state updates:
    - `EntryDraftProvider#setDraft(...)` keeps plaintext in memory
    - toast “Saved”

### 5.2 Open/edit entry (target)

**Flow**

```
Route /entry?path=...
  → try load ciphertext from IndexedDB first (fast/offline)
  → if not present AND API reachable: fetch from backend and store ciphertext to IndexedDB
  → decrypt after load into EntryDraftProvider
  → edit plaintext in memory
  → encrypt on save
```

### 5.3 Analysis/recommendations endpoints (explicit boundary)

These endpoints intentionally receive plaintext:
- `POST /api/analysis`
- `POST /api/recommendations`

Keep this behavior explicit in code:
- Make sure entry persistence continues to send ciphertext only.
- Treat analysis/recommendations calls as “compute-only” and never persist their plaintext inputs locally.

---

## 6) IndexedDB Design + Sync Queue

### 6.1 Database name, version, and stores

- DB name: `sentimentscribe.offline`
- DB version: `1`

Object stores (minimum viable):

1) `entries`
- Key: compound `[userId, storagePath]` (or a single `entryKey = "${userId}:${storagePath}"`)
  - In this repo, `storagePath` is the effective **entry ID** used everywhere (routes, API, and persistence identity).
- Value: ciphertext envelope + server timestamps + local metadata

2) `syncQueue`
- Key: auto-increment `id` (preserves ordering)
- Value: `{ userId, op, storagePath, payload?, enqueuedAt, retryCount, lastError }`

### 6.2 Record shapes

**IndexedDbEntryRecord**

- Identity:
  - `userId: string`
  - `storagePath: string`
- Server metadata (nullable while offline-created):
  - `createdAt: string | null`
  - `updatedAt: string | null`
- Ciphertext payload (same as backend fields):
  - `titleCiphertext`, `titleIv`, `bodyCiphertext`, `bodyIv`, `algo`, `version`
- Local metadata:
  - `dirty: boolean` (has unsynced local changes)
  - `deletedAt: string | null` (tombstone; not shown in list)

**IndexedDbSyncQueueItem**

- `id: number` (auto-increment)
- `userId: string`
- `op: 'upsert' | 'delete'`
- `storagePath: string`
- For upsert: `payload: EntryRequest` (ciphertext-only)
- `enqueuedAt: string`
- `retryCount: number`
- `lastAttemptAt: string | null`
- `lastError: string | null`

### 6.3 Indexes (for fast list + filters)

On `entries` store:
- index by `userId`
- index by `[userId, updatedAt]` (for sorting newest-first)
- index by `[userId, storagePath]` (if not already the primary key shape)
- optional index by `[userId, dirty]` (to quickly count pending)

On `syncQueue` store:
- index by `userId`

### 6.4 What constitutes “dirty”

- `dirty = true` whenever:
  - a ciphertext entry is written locally as a result of a user save while offline or before confirmation from server
  - a delete tombstone is created locally (until server confirms deletion)
- `dirty = false` when:
  - the latest queued operation for that entry has been successfully pushed and acknowledged

---

## 7) Sync Engine (online detection + push/pull)

### 7.1 Online/offline detection strategy

Use a layered approach:

1) `navigator.onLine` (cheap, imperfect)
2) `window` events:
   - `online` → attempt queue flush
   - `offline` → set offline UI indicator
3) Optional health check:
   - `GET /api/health` (already exists: `backend/src/main/java/com/sentimentscribe/web/HealthController.java`)
   - Use it to distinguish “online but API down” vs “offline”.

### 7.2 Push behavior (required)

Flush queue in order (lowest `id` first):

- `upsert`:
  - Send ciphertext-only `EntryRequest` to the backend.
  - Prefer using **PUT** for idempotency if backend accepts it for “create if missing”.
    - If backend rejects PUT-create, fall back to POST for first-time creates (requires knowing if the entry exists; avoid if possible).
- `delete`:
  - Call `DELETE /api/entries?path=...`

On success:
- remove the queue item
- update `entries` record:
  - set `dirty=false`
  - update `updatedAt` using server `EntryResponse.updatedAt` (for upsert)
  - if delete succeeded: remove the local record or keep a tombstone (MVP recommendation: remove)

### 7.3 Retry/backoff (minimal)

- If a request fails due to network/API unreachable:
  - stop the flush early (do not churn through the queue)
  - mark the item with `lastError` and `lastAttemptAt`
  - retry on next online event or manual “Sync now”
- Retry count:
  - increment on each attempt, cap at small number (e.g., 5) before surfacing “Needs attention” UI text.

### 7.4 Conflict resolution (MVP: last-write-wins)

Single-device MVP means conflicts are rare because the server should only change due to this client’s own pushes.

Still define behavior:
- Server `updatedAt` is authoritative once a push succeeds.
- If later you enable multi-device pull, you must resolve:
  - local dirty edits vs server newer edits using `updatedAt` (or a proper version vector / etag).

### 7.5 Pull behavior (explicitly out-of-scope unless multi-device)

- Do **not** implement continuous pull/merge.
- Allowed “bootstrap pull” for single-device:
  - If local cache is empty for the user and API is reachable, fetch `GET /api/entries` and store summaries.
  - Fetch full entry bodies only on-demand (`/api/entries/by-path`) and cache ciphertext.

---

## 8) Offline-Resilient UI

### 8.1 Home list behavior

Target priority:

1) Load from IndexedDB first (fast, works offline).
2) If API reachable:
   - refresh list from server and update IndexedDB **only for entries that are not dirty locally**
   - keep tombstones honored (don’t resurrect locally-deleted entries without multi-device support)
3) If API unreachable:
   - show “Offline” indicator
   - keep rendering from IndexedDB

Rendering rules:
- If locked (no key): show `Encrypted entry` placeholders (already used today).
- If unlocked: decrypt titles for display only; do not persist plaintext.

### 8.2 Entry open behavior

Target priority:

1) Try IndexedDB entry record (works offline).
2) If missing and API reachable: fetch from backend, persist ciphertext to IndexedDB, then decrypt.
3) If missing and offline: show a clear error (“Entry not available offline yet.”) + back button.

### 8.3 UI indicators

- Always-visible Offline badge when:
  - browser is offline OR health check fails
- Optional sync pending count:
  - count of `syncQueue` items for current user

Where to render:
- `RootLayout` in `frontend/src/routes.tsx` (global, across pages), or
- page headers in `HomeMenuPage` / `DiaryEntryPage` (minimal)

---

## 9) Step-by-Step Implementation Plan (execution ready)

> After completing each step, update `crypto-offline-changes.md` under the matching step header.

### Step 0 — Baseline + guardrails

**Goal**
- Establish a repeatable manual verification routine and ensure no plaintext leaks into persistence paths.

**Files**
- Read/confirm only:
  - `frontend/src/pages/HomeMenuPage.tsx`
  - `frontend/src/pages/DiaryEntryPage.tsx`
  - `frontend/src/api/entries.ts`
  - `frontend/src/api/http.ts`
  - `frontend/src/crypto/*`

**Data flow changes**
- None (documentation-only step).

**Verification checklist**
- Manual:
  - Confirm current save sends ciphertext fields (inspect network payload in devtools).
  - Confirm refresh requires unlock again (key is memory-only).

---

### Step 1 — Consolidate crypto API surface (deriveKey → encrypt → decrypt)

**Goal**
- Provide a stable, minimal crypto API matching requirements without changing cryptography semantics.

**Files to create/modify**
- Create: `frontend/src/crypto/diaryCrypto.ts`
- (Optional) Modify exports in: `frontend/src/crypto/envelope.ts`, `frontend/src/crypto/kdf.ts`
- Update tests: `frontend/src/crypto/__tests__/crypto.test.ts`

**Data flow changes**
- Pages and offline modules call `diaryCrypto.deriveKey/encrypt/decrypt` (and `encryptEntry/decryptEntry`) instead of reaching into multiple crypto files.

**Verification checklist**
- Automated:
  - unit tests: deriveKey + encrypt/decrypt round-trip
  - tests assert IV is random length 12 and envelope includes `{ version, algo }`

---

### Step 2 — Lock/unlock lifecycle hardening (clear key on lock/logout/tab close)

**Goal**
- Ensure derived keys are cleared on:
  - explicit lock
  - auth/session clear (logout or 401)
  - tab close/refresh (best-effort)

**Files to modify/create**
- Modify: `frontend/src/state/e2ee.tsx` (beforeunload hook; explicit clear)
- Modify: `frontend/src/pages/HomeMenuPage.tsx` (add Lock button calling `useE2ee().clear()`)
- Modify: `frontend/src/pages/DiaryEntryPage.tsx` (add Lock button calling `useE2ee().clear()`)
- (Optional) Modify: `frontend/src/state/entryDraft.tsx` (clear plaintext draft on lock)

**Data flow changes**
- Lock action becomes an explicit transition: unlocked → locked, enforced by routing redirect to `/unlock`.

**Verification checklist**
- Manual:
  - unlock → open `/home` → click Lock → redirected to `/unlock`
  - unlock → refresh → redirected to `/unlock`
  - observe that entry plaintext fields are cleared/reset on lock (if implemented)

---

### Step 3 — Add IndexedDB foundation (schema + helper)

**Goal**
- Create a minimal IndexedDB wrapper with an upgrade path (versioned schema).

**Files to create**
- Create: `frontend/src/offline/db.ts` (open DB, upgrade, transaction helpers)
- Create: `frontend/src/offline/types.ts` (record and queue types)

**Data flow changes**
- None yet (no pages wired). This step only establishes local persistence primitives.

**Verification checklist**
- Manual (devtools):
  - open the app and ensure the DB exists under Application → IndexedDB (`sentimentscribe.offline`)
- Automated (optional):
  - add unit tests using `fake-indexeddb` (if you choose to add it as a dev dependency)

---

### Step 4 — Implement local encrypted entries repository (ciphertext-only)

**Goal**
- Provide typed functions to read/write ciphertext entries without any plaintext storage.

**Files to create**
- Create: `frontend/src/offline/entriesRepo.ts`

**Data flow changes**
- Enables “IndexedDB-first” reads for the UI and enables offline saves to persist immediately.

**Verification checklist**
- Manual:
  - write a small dev-only call site (or temporary button) to store/fetch an entry record and confirm fields are ciphertext only
  - inspect IndexedDB record contents and verify no plaintext appears

---

### Step 5 — Implement sync queue repository

**Goal**
- Track pending creates/updates/deletes (ciphertext-only) in a durable queue.

**Files to create**
- Create: `frontend/src/offline/syncQueueRepo.ts`

**Data flow changes**
- All offline mutations become durable:
  - save → enqueue `upsert`
  - delete → enqueue `delete`

**Verification checklist**
- Manual:
  - enqueue an item and verify it exists and increments pending count correctly

---

### Step 6 — Implement sync engine (push-only)

**Goal**
- Flush the sync queue to the backend when online, updating local records on success.

**Files to create/modify**
- Create: `frontend/src/offline/syncEngine.ts`
- (Optional) Modify: `frontend/src/api/entries.ts` to expose an idempotent “upsert” API wrapper (prefer PUT).

**Data flow changes**
- Queue items become actual backend requests, but plaintext never enters these codepaths.

**Verification checklist**
- Manual:
  - simulate offline (devtools “Offline”), save an entry, confirm queue grows
  - switch back online, confirm the queue flushes and the backend has the ciphertext

---

### Step 7 — Add offline/sync state + UI indicator

**Goal**
- Surface offline status and pending sync count in the UI, and provide a single place to trigger sync.

**Files to create/modify**
- Create: `frontend/src/state/offline.tsx` (context: `isOffline`, `pendingCount`, `syncNow`)
- Modify: `frontend/src/App.tsx` (wrap providers with `OfflineProvider` near `AuthProvider`)
- Create: `frontend/src/components/OfflineIndicator.tsx` (badge + optional count)
- Modify: `frontend/src/routes.tsx` (render indicator in `RootLayout`)
- (Optional) Modify: `frontend/src/App.css` (styles for badge)

**Data flow changes**
- UI gains visibility into whether the app is operating from local cache and whether sync is pending.

**Verification checklist**
- Manual:
  - toggle offline/online and confirm the badge updates
  - make a queued change and confirm the pending count increments/decrements

---

### Step 8 — Home list: IndexedDB-first, API fallback/refresh

**Goal**
- Home list works fully offline by loading from IndexedDB when API is unreachable.

**Files to modify**
- Modify: `frontend/src/pages/HomeMenuPage.tsx`

**Data flow changes**
- Replace “API-only list” with:
  - load ciphertext summaries from IndexedDB
  - decrypt titles if unlocked
  - attempt server refresh when online (without overwriting dirty local entries)

**Verification checklist**
- Manual:
  - while online: load home, confirm it still shows entries
  - go offline: refresh page, unlock, confirm home list loads from IndexedDB

---

### Step 9 — Entry page: offline load + offline save (enqueue + attempt sync)

**Goal**
- Entry open/edit/save works offline using IndexedDB as the source of truth.

**Files to modify**
- Modify: `frontend/src/pages/DiaryEntryPage.tsx`
- Modify: `frontend/src/state/entryDraft.tsx` (generate `storagePath` for new entries)
  - Recommended: `entries/${crypto.randomUUID()}.json` (or `.txt`) so offline creates have stable IDs.

**Data flow changes**
- Entry load:
  - tries IndexedDB first; falls back to server if missing
- Entry save:
  - encrypt → write ciphertext to IndexedDB → enqueue upsert → attempt sync
  - UI updates immediately from local data

**Verification checklist**
- Manual:
  - create new entry while offline: it appears in home list and opens again after refresh (offline)
  - go online: queued entry syncs and remains accessible

---

### Step 10 — Delete flow: offline delete + queued delete

**Goal**
- Deletes work offline by removing locally and syncing deletion when online.

**Files to modify**
- Modify: `frontend/src/pages/HomeMenuPage.tsx` (delete handler)
- Modify: `frontend/src/offline/entriesRepo.ts` (tombstone/remove support)
- Modify: `frontend/src/offline/syncQueueRepo.ts` (enqueue delete)

**Data flow changes**
- Delete confirm:
  - update IndexedDB immediately (remove/tombstone)
  - enqueue delete
  - attempt sync

**Verification checklist**
- Manual:
  - offline delete: entry disappears locally and stays gone after refresh
  - back online: server deletion happens and queue clears

---

### Step 11 — Tests + verification matrix

**Goal**
- Add targeted automated coverage where feasible; otherwise document manual verification procedures clearly.

**Files to create/modify**
- Add offline unit tests (optional but recommended):
  - `frontend/src/offline/__tests__/*`
  - `frontend/package.json` devDependencies (if adding `fake-indexeddb`)
- Update existing page tests if data loading strategy changes:
  - `frontend/src/pages/__tests__/*`

**Data flow changes**
- None (tests only).

**Verification checklist**
- Automated:
  - crypto round-trip tests remain green
  - entries page/home page tests updated for IndexedDB-first flow
- Manual:
  - offline: load home, open entry, edit, save, delete, refresh, unlock
  - online: queue flush, pending count drops to 0

---

## 10) Risks, Gotchas, and Debugging Guide

### Web Crypto pitfalls

- AES-GCM decryption failures are opaque (“OperationError”); always catch and surface a user-friendly message.
- Ensure IV length is 12 bytes; other lengths work but are non-standard and easier to misuse.
- Avoid extracting keys; keep `CryptoKey` non-extractable (current code does this).

### Base64 pitfalls

- `btoa/atob` work on binary strings; always convert bytes carefully (current `base64.ts` does).
- Beware of accidentally base64-encoding UTF-8 text directly instead of encrypted bytes.

### IndexedDB pitfalls

- Schema upgrades can be blocked by another open tab:
  - handle `onblocked` and instruct user to close other tabs (or retry).
- Transactions auto-commit when the event loop yields; keep logic inside a single transaction scope where needed.
- Key design matters:
  - prefer stable keys (`[userId, storagePath]`) and avoid needing server-generated IDs for offline creates.

### Sync debugging

- Distinguish:
  - “browser offline” vs “API down” (use `/api/health` ping)
- Inspect queue:
  - Application → IndexedDB → `sentimentscribe.offline` → `syncQueue`
- Common failure modes:
  - 401: auth expired → `request()` clears session; queue will remain until user logs in again
  - 400: payload validation errors → queue item becomes “stuck” (surface lastError to user)
