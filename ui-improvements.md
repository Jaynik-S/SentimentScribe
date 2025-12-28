# UI Improvements

## Summary of changes
- Added consistent headers, breadcrumbs, and anchored navigation across pages.
- Replaced the home entries table with a card-based list and friendly empty state.
- Strengthened entry page hierarchy with status indicators, integrated keywords, and a roomier editor.
- Reworked recommendations into grouped sections with contextual framing and clearer actions.
- Refined global styling, spacing, and interaction feedback for a calmer companion feel.

## Screens impacted
- Login, Register, Unlock
- Home Menu
- Diary Entry
- Recommendations

## Key components created/modified
- `frontend/src/components/EntriesTable.tsx`
- `frontend/src/components/MoviesTable.tsx`
- `frontend/src/components/SongsTable.tsx`
- `frontend/src/components/DeleteEntryModal.tsx`
- `frontend/src/pages/HomeMenuPage.tsx`
- `frontend/src/pages/DiaryEntryPage.tsx`
- `frontend/src/pages/RecommendationPage.tsx`
- `frontend/src/pages/LoginPage.tsx`
- `frontend/src/pages/RegisterPage.tsx`
- `frontend/src/pages/UnlockPage.tsx`
- `frontend/src/App.css`
- `frontend/src/index.css`

## Before/after behavior notes
- Home Menu: table list becomes a card list with hover/focus affordances, a subtle recent marker, and storage paths tucked into details.
- Diary Entry: keywords toggle sits under the title, save is clearly primary, and status chips show saved vs unsaved plus analysis state.
- Recommendations: grouped song/movie sections now include a contextual header, top pick badges, and in-card action buttons.
- Global: consistent header/nav placement, clearer hierarchy, and richer feedback through toasts and subtle motion.

## How to verify manually
1. Sign in, register, and unlock pages: confirm headers/breadcrumbs and form flows still work.
2. Home Menu: entries render as cards, hover states and keyboard focus work, delete confirmation appears, empty state shows when list is empty.
3. Diary Entry: edit title/text to see unsaved state, save to see confirmation, analyze keywords to see status update.
4. Recommendations: request recommendations, verify header context and grouped sections, open Spotify links, use Back to Entry.
5. Resize to mobile width and confirm layout stacks cleanly.

## Final cleanup pass
- Pages affected: Login, Register, Unlock, Home Menu, Diary Entry, Recommendations.
- Removed/relocated: breadcrumb subheadings, instructional text, storage path details, entry subtitle dates, recent tags, hero callout box; moved status and updated info into Entry Details.
- Functionality unchanged: entry loading/saving/analyzing, delete confirmation, recommendations flow all remain intact.

## Home menu polish pass
- Header: aligned with Diary Entry styling, consistent divider, and aligned Lock/New Entry controls.
- Entry previews: added body snippets with clean truncation, kept date bubbles and secondary delete action.
- Removed: keywords dropdowns from the Home list only; no functional changes elsewhere.
