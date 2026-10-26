# Plan 4 — iOS emoji search

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:executing-plans (inline). Steps use checkbox (`- [ ]`) syntax.

**Goal:** a "Search Emoji" field on top of the iOS emoji screen; tapping it shows the letter keyboard with the search field and a row of matching emoji above it, as on iOS 27.

**Architecture:** names and keywords come from Unicode CLDR annotations (ru, en, kk, incl. derived ones for flags), turned into `assets/emoji_search/<lang>.txt` by `tools/emoji/make_emoji_search.py`, filtered to the emoji in our lists. `IosEmojiSearchIndex` (pure) matches all three languages at once. While search is active, `LatinIME.onEvent`/`onTextInput` route typed characters into the query (`IosEmojiQuery`, pure) instead of the app; switching keys (shift, 123, globe) work as usual. `IosEmojiSearchBar` (results row + field) sits above the letters in `main_keyboard_frame`; picking a result commits the emoji to the app and keeps the search open.

**Spec:** `docs/superpowers/specs/2026-10-26-ios-keyboard-design.md` (emoji screen like iOS); user decision 2026-10-27 «давай поиск эмодзи».

## Global Constraints

- iOS style only. CLDR data is under the Unicode License v3 — note in `docs/third-party.md`.
- Search all of ru/en/kk at once (Cyrillic queries hit ru/kk, Latin hit en); case- and ё/е-insensitive; every query word must be a prefix of some word of the emoji's name or keywords.
- Ranking: emoji whose name matches before keyword-only matches, then the emoji screen order.
- Empty query shows frequently used emoji; no match shows «Эмодзи не найдены» (localized).
- Exit: ✕ in the field, Enter, or the emoji key → back to the emoji screen; hiding the keyboard ends the search.

## Review Focus

1. Text must never leak into the app while searching (letters, space, delete, popup letters like ё/қ).
2. Picking a result must reach the app even though input is routed to the query.
3. Keyboard hidden or app switched mid-search — next open is a normal keyboard.
4. Insets: the app's content must not be covered by the search bar.
5. Long-press skin tones on a result.

---

### Task 1: Data and index
**Files:** `tools/emoji/make_emoji_search.py`, `assets/emoji_search/{en,ru,kk}.txt`, `latin/ios/IosEmojiSearchIndex.kt`, test `IosEmojiSearchIndexTest.kt`, `docs/third-party.md`.
- [ ] Tests: prefix match on name words («улыб» → 😀), multi-word AND («флаг каз» → 🇰🇿), ё→е, name match ranks before keyword match, order preserved otherwise, Kazakh («ту қаз» → 🇰🇿), empty query → empty.
- [ ] Generate assets, implement, commit `feat: emoji search index from CLDR`.

### Task 2: Query routing
**Files:** `latin/ios/IosEmojiQuery.kt`, test `IosEmojiQueryTest.kt`, `LatinIME.java`.
- [ ] Tests: letter/space append, delete removes last code point (emoji-safe), Enter → `Exit`, emoji key → `Exit` + pass through, functional keys (shift, symbols, globe) → `PassThrough`, text input appends.
- [ ] Implement, commit `feat: route typing into emoji search`.

### Task 3: UI
**Files:** `latin/ios/IosEmojiSearchBar.kt`, `keyboard/emoji/IosEmojiPalette.kt` (top field), `EmojiPalettesView.java`, `KeyboardSwitcher.java`, `LatinIME.java` (insets, finish), `res/layout/main_keyboard_frame.xml`, strings.
- [ ] Device check: search ru/en/kk words, skin-tone popup on a result, pick → emoji in the field, ✕/Enter/emoji key back, hide mid-search, nothing leaked into the app; commit `feat: iOS emoji search UI`.
