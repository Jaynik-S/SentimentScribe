# Crypto + Offline Changes (Step-by-Step Log Template)

> Rule: after completing **each** step from `crypto-offline-plan.md` Section 9, document what changed here under the matching step.

## Step 0 - Baseline + guardrails

- Files changed: none
- Summary: reviewed current entry list/load/save/delete flows and crypto helpers; no code changes in Step 0
- UI flow notes: Home loads via `listEntries()` and decrypts titles client-side when key is present; entry page loads via `getEntryByPath()` and decrypts envelope; save encrypts then POST/PUTs ciphertext fields; delete calls `deleteEntry()` then reloads list
- Crypto notes: AES-GCM with 12-byte IV, envelope version 1 with title/body ciphertext+IV; PBKDF2-SHA256 derive key, in-memory only
- IndexedDB notes: none (not yet implemented)
- Sync notes: none (not yet implemented)
- Verification: manual devtools/network checks not run in this step

## Step 1 — Consolidate crypto API surface (deriveKey → encrypt → decrypt)

- Files changed: `frontend/src/crypto/diaryCrypto.ts`, `frontend/src/state/e2ee.tsx`, `frontend/src/pages/HomeMenuPage.tsx`, `frontend/src/pages/DiaryEntryPage.tsx`, `frontend/src/crypto/__tests__/crypto.test.ts`, `frontend/src/pages/__tests__/HomeMenuPage.test.tsx`, `frontend/src/pages/__tests__/DiaryEntryPage.test.tsx`
- Summary: added a consolidated crypto facade and moved UI/tests onto it while keeping existing envelope/AES-GCM primitives intact
- UI flow notes: Home list decrypts titles via `diaryCrypto.decrypt` using entry algo/version; entry page encrypts/decrypts via `diaryCrypto.encryptEntry/decryptEntry`; unlock uses `diaryCrypto.deriveKey` and validates KDF
- Crypto notes: new `diaryCrypto` wrapper exposes `deriveKey`, `encrypt`, `decrypt`, `encryptEntry`, `decryptEntry`; single-field encrypt includes `{ version, algo }` and decrypt enforces expected version/algo
- IndexedDB notes: none
- Sync notes: none
- Verification: tests updated but not run in this step

## Step 2 - Lock/unlock lifecycle hardening (clear key on lock/logout/tab close)

- Files changed: `frontend/src/state/e2ee.tsx`, `frontend/src/state/entryDraft.tsx`, `frontend/src/pages/HomeMenuPage.tsx`, `frontend/src/pages/DiaryEntryPage.tsx`, `frontend/src/App.css`
- Summary: added explicit Lock actions, draft clearing helper, and a beforeunload hook to clear the in-memory key
- UI flow notes: Home and entry headers include a Lock button that clears draft + key and navigates to `/unlock`; header actions grouped for layout
- Crypto notes: key is cleared on tab close via `beforeunload` listener in `E2eeProvider`
- IndexedDB notes: none
- Sync notes: none
- Verification: not run in this step

## Step 3 - Add IndexedDB foundation (schema + helper)

- Files changed: `frontend/src/offline/db.ts`, `frontend/src/offline/types.ts`
- Summary: added a versioned IndexedDB wrapper and type definitions for offline entry and sync queue records
- UI flow notes: none
- Crypto notes: none
- IndexedDB notes: DB `sentimentscribe.offline` v1 with `entries` (compound key `[userId, storagePath]` and indexes by user, updatedAt, storagePath, dirty) and `syncQueue` (auto-increment `id`, index by userId)
- Sync notes: none
- Verification: not run in this step

## Step 4 - Implement local encrypted entries repository (ciphertext-only)

- Files changed: `frontend/src/offline/entriesRepo.ts`
- Summary: added a typed repository for reading/writing ciphertext entry records in IndexedDB
- UI flow notes: none
- Crypto notes: ciphertext-only records stored/returned; no plaintext handling
- IndexedDB notes: repository uses `entries` store and userId index; list returns newest-first and can exclude tombstones
- Sync notes: none
- Verification: not run in this step

## Step 5 — Implement sync queue repository

- Files changed:
- Summary:
- UI flow notes:
- Crypto notes:
- IndexedDB notes:
- Sync notes:
- Verification:

## Step 6 — Implement sync engine (push-only)

- Files changed:
- Summary:
- UI flow notes:
- Crypto notes:
- IndexedDB notes:
- Sync notes:
- Verification:

## Step 7 — Add offline/sync state + UI indicator

- Files changed:
- Summary:
- UI flow notes:
- Crypto notes:
- IndexedDB notes:
- Sync notes:
- Verification:

## Step 8 — Home list: IndexedDB-first, API fallback/refresh

- Files changed:
- Summary:
- UI flow notes:
- Crypto notes:
- IndexedDB notes:
- Sync notes:
- Verification:

## Step 9 — Entry page: offline load + offline save (enqueue + attempt sync)

- Files changed:
- Summary:
- UI flow notes:
- Crypto notes:
- IndexedDB notes:
- Sync notes:
- Verification:

## Step 10 — Delete flow: offline delete + queued delete

- Files changed:
- Summary:
- UI flow notes:
- Crypto notes:
- IndexedDB notes:
- Sync notes:
- Verification:

## Step 11 — Tests + verification matrix

- Files changed:
- Summary:
- UI flow notes:
- Crypto notes:
- IndexedDB notes:
- Sync notes:
- Verification:
