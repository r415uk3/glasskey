# Glasskey settings: iOS 27 redesign

Date: 2026-10-27. Status: draft for the user's review.
Mockups: `.superpowers/brainstorm/49827-1793051131/content/settings-v2.html` (structure), `settings-glass-v3.html` (glass) — approved by the user ("все окей").

## Intent

Glasskey's keyboard already looks and behaves like iOS 27; its settings app is still HeliBoard's Material screen with ~10 sections and over a hundred options. The user wants the settings to look like iOS 27 Settings, **simplified to what is actually needed**, with **Liquid Glass** — not only on the navigation chrome but also on the setting groups and the switches.

Success: opening Glasskey's settings feels like opening Settings › General › Keyboard on an iPhone 27 — a short list of familiar switches, nothing HeliBoard-specific in sight, glass that visibly refracts.

## Goals

1. iOS 27 look: large title that collapses into a centered title on scroll, inset grouped lists with ~26dp corners, small grey section headers and footers, chevrons with grey values, green iOS switches, iOS-style checkmark lists for choices.
2. Liquid Glass (user decision):
   - glass circular back button and glass top bar once the large title has scrolled away;
   - glass search field floating at the bottom of the main screen;
   - **every settings group is a glass panel**;
   - **switches**: plain iOS pill at rest; while pressed / dragged the thumb becomes a glass lens that grows and refracts what is under it, as in iOS 26/27.
   - Behind the glass a soft blurred colour background (deep blue / violet / teal blobs on near-black in dark mode, a pale equivalent in light mode) so the glass has something to refract (approved in mockup v3).
3. Only the needed settings (see Structure). Everything else disappears from the UI but keeps its current (Glasskey default) value and keeps working.
4. Settings UI follows the system light/dark mode; texts in English, Russian, Kazakh like the rest of Glasskey.

## Non-goals

- No change to keyboard behaviour or defaults (except where a kept switch is toggled by the user).
- No new keyboard features. Reordering languages is a settings-UI feature but changes globe order (see below).
- No Google Play specifics.

## Anti-goals

- HeliBoard terminology or Material widgets showing through (toolbars, Material switches, ripple-heavy rows, FABs).
- Glass that is just a grey translucent box (no refraction / no highlight edges) on supported devices.
- Losing access to something the user needs for their languages: adding a keyboard language and adding a dictionary must stay possible.

## Structure (user-approved via mockups)

**Main screen — large title "Glasskey"**
- Card "Включить Glasskey" — only while Glasskey is not enabled or not the current input method; tapping it runs the existing enable/select flow (HeliBoard's welcome wizard logic).
- Group: **Клавиатуры** (value: number of languages) › · **Замена текста** › · **Оформление** (value: font name) ›
- Header «ВСЕ КЛАВИАТУРЫ», switches: Автопрописные (`PREF_AUTO_CAP`), Автокоррекция (`PREF_AUTO_CORRECTION`), Предиктивный набор (`PREF_SHOW_SUGGESTIONS`), Просмотр символов (`PREF_POPUP_ON`), Быстрая клавиша «.» (`PREF_KEY_USE_DOUBLE_SPACE_PERIOD`) with footer «Двойное нажатие пробела вставляет точку и пробел».
- Header «ОТКЛИК»: Звук (`PREF_SOUND_ON`), Вибрация (`PREF_VIBRATE_ON`).
- Group: **О Glasskey** › (version, source link, licenses incl. `docs/third-party.md` items).
- Floating glass search field at the bottom: searches the kept settings only.

**Клавиатуры**
- List of enabled keyboard languages, each row: language name + layout name (grey) ›; drag to reorder (iOS edit mode or long-press drag); swipe/edit to remove (at least one must stay).
- Footer: «Глобус переключает языки в этом порядке».
- «Добавить клавиатуру…» (blue) → language list → layout choice.
- Language row › → layout choice for that language (checkmark list).
- **Словари** › → existing dictionary management (add a dictionary file for a language), restyled. Footer «Подсказки для языков без встроенного словаря».

**Замена текста** — personal dictionary entries (word + shortcut), add / edit / delete, iOS style. Global like iOS: new entries go to "all languages"; existing per-language entries are listed too, with their language as a grey caption (no migration).

**Оформление** — Тема (Как в системе / Светлая / Тёмная, checkmark list), Шрифт (System, Inter, Golos Text, Onest, Noto Sans), Ряд цифр (`PREF_SHOW_NUMBER_ROW`), Размытие фона (keyboard glass on/off; new pref, default on — when off the panel uses the opaque iOS colour).

**Removed from the UI**: Toolbar, Secondary layouts, Colors / background image, split keyboard, height/gap/padding scales, gesture typing, gesture data, space/delete swipe options, clipboard history, hints, popup order, emoji options, backup/restore, Advanced, Debug, experimental. Their current values stay as they are.

## Decided by the user

- Simplify, keep only what is needed; Liquid Glass including groups and switches; coloured blurred background behind glass (mockup v3 "все окей").
- Emoji search in all languages (done 2026-10-27); other languages must remain addable, so «Словари» stays inside «Клавиатуры».

## Builder's calls (marked as mine, the user may still change them)

- Globe order = the order in «Клавиатуры» (drag to reorder) instead of today's alphabetical order — this is the iOS behaviour.
- "Тема" maps to the existing day/night theme prefs of the iOS keyboard style.
- "Размытие фона" is a new switch for the keyboard glass.
- Backup/restore and Advanced are dropped entirely (no hidden escape hatch).
- «Замена текста» is global (see Structure).
- Source link in «О Glasskey»: `https://github.com/r415uk3/glasskey` (repo not created yet; one string to change if the name differs).
- Glass implementation: a Compose backdrop/liquid-glass library (e.g. Kyant's `backdrop`) or an own `RuntimeShader`; real refraction on Android 13+, blur-only on Android 12, plain translucent fill below. minSdk stays 21.

## Constraints

- Jetpack Compose settings (existing `settings/` package); minSdk 21, compileSdk 37; app published on GitHub only.
- Strings in `values`, `values-ru`, `values-kk`.
- Keyboard style decisions from earlier specs stand (`2026-10-26-ios-keyboard-design.md`).

## Left to the builder

Exact spacing/typography values (follow iOS 27 metrics scaled to dp), animation curves, how the large title collapses, search behaviour details, how removed screens are kept or deleted in code (upstream-merge friendliness vs dead code).
