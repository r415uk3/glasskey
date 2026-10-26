# Plan 1 — iOS look, layouts, defaults, globe strip

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn the HeliBoard fork into an installable keyboard that looks like the iOS 27 keyboard (light/dark), with iOS layouts for en/ru/kk, iOS-like defaults out of the box, no toolbar, and a strip under the keys with a working globe button.

**Architecture:** Keep HeliBoard's engine and View-based renderer. Add a 6th theme style `iOS` (themeId 5) with its own XML style family, drawables and two color sets; make dp-based geometry an iOS-only branch in `KeyboardParams`; ship iOS layout files and point `Defaults` at them; add one new view (`IosBottomBar`) under the keys in `main_keyboard_frame`, wired through `KeyboardSwitcher` and accounted for in `LatinIME.onComputeInsets`. All iOS behaviour is selected by the style, so stock HeliBoard styles keep working (keeps upstream merges sane).

**Tech Stack:** Java/Kotlin (Android View system, not Compose, for the keyboard), Gradle KTS, NDK 28.0.13004108 (already in the build), Robolectric 4.16.1 unit tests, adb to the user's phone.

**Spec:** `docs/superpowers/specs/2026-10-26-ios-keyboard-design.md`

**Out of scope here (later plans):** Plan 2 — mic/voice input in the strip, space long-press trackpad. Plan 3 — iOS emoji screen. In this plan the mic slot exists in the strip but stays `GONE`.

## Global Constraints

- applicationId `io.github.r415uk3.glasskey` (release), `.debug` suffix kept for debug types; Kotlin/Java packages and `namespace = "helium314.keyboard.latin"` stay unchanged. App/IME label: **Glasskey** (user decision 2026-10-26).
- Keep GPL-3.0 and all existing copyright headers; new files get `SPDX-License-Identifier: GPL-3.0-only`.
- Never ship Apple fonts or SF Symbols. Fonts (user decision): selectable in settings — System, **Inter** (default), Golos Text, Onest, Noto Sans; all SIL OFL 1.1, bundled in `res/font/`; HeliBoard's "custom font from file" stays and overrides the selection.
- Figma values (pt → dp 1:1, from file `yhjiyzvjaydTpc7tqsEyg2`):
  - Dark: panel `#DB1C1C1C` (rgba 28,28,28,.86); keys incl. functional `#45FFFFFF` (white .27); text `#FFFFFFFF`; suggestion divider `#4DFFFFFF`.
  - Light: panel `#BFDADADA` (rgba 218,218,218,.75) — use opaque `#DADADA`-ish only if translucency shows app content badly; keys `#E3FFFFFF` (white .89); text `#A6000000` (black .65); suggestion divider `#1A000000`.
  - Active action key (Go/Search/Send/Next/Done): `#0088FF`, white glyph. Plain Enter: key color, glyph at normal text color.
  - Key radius 8.5dp; key height 45dp; horizontal gap 6.5dp; row gap 10dp; row 2 side inset 16dp; panel top corner radius 27dp; panel side padding 4dp, top padding 8dp; letter size 25sp (dark) / 23sp (light) → use **24sp** for both; label size (123, ABC, .com) 16sp.
  - Suggestions: 3 slots, 18sp, divider 1×24dp.
  - Numpad: 3×4, key height 50dp, gaps 6dp, digit 23sp, letters 10sp bold letter-spacing 0.2em.
- The phone under test: vivo V2453A, OriginOS 6, Android 16, gesture nav, 1260×2800 @560dpi, connected over adb. On-device checks happen there, not on an emulator.
- Existing user installs: official HeliBoard (`helium314.keyboard`) stays installed side by side; nothing here may collide with its authorities.

## Review Focus

1. **Input fields with special types** (email, URL, search, password, number, phone) — the bottom row must change per the Figma Input Type frame, and numeric/phone fields must show the iOS numpad, not the letter layout. → tests in Task 5.
2. **Switching languages while shifted / on the symbols page** — globe must switch subtype and land on that language's alphabet page; long-press must open the picker even when only HeliBoard is the enabled IME. → test in Task 7.
3. **Landscape and split-screen** — dp geometry must not overflow: when 4×(45+10)dp + paddings exceeds `config_max_keyboard_height` (46%p), rows scale down instead of being clipped. → test in Task 3.
4. **Gesture-nav insets on OriginOS** — the bottom strip must not sit under the gesture handle and the keyboard must not leave a gap of nav-bar color; `onComputeInsets` must still let the app resize correctly. → device check in Task 7 + Task 9.
5. **Incognito / password fields** — the suggestion strip must not grow the toolbar expand key (HeliBoard shows it in incognito). → test in Task 8.

---

### Task 1: Rebrand so it installs next to HeliBoard

**Files:**
- Modify: `app/build.gradle.kts:13` (applicationId), `:69` (APK name `Glasskey_${versionName}-${buildType}.apk`)
- Modify: `app/src/main/res/values/donottranslate.xml:73` (`english_ime_name` → `Glasskey`)
- Modify: `app/src/main/res/values/clip_provider.xml:3` → `io.github.r415uk3.glasskey.clipprovider`; `app/src/main/res/values/gesture_data.xml:3` → `io.github.r415uk3.glasskey.provider`; same two in `src/debug/res/values/` and `src/debugNoMinify/res/values/` with `.debug` inserted before the last segment.
- Modify: `src/debug/res/values/strings.xml:6-8` labels to `Glasskey debug`.
- Test: `app/src/test/java/helium314/keyboard/RebrandTest.kt`

**Interfaces:** Produces nothing code-level; later tasks rely on the app being installable as `io.github.r415uk3.glasskey.debug`.

- [ ] **Step 1: Write the failing test** — Robolectric `RebrandTest`:
  - `applicationIdIsOurs`: `assertTrue(BuildConfig.APPLICATION_ID.startsWith("io.github.r415uk3.glasskey"))`
  - `authoritiesAreNotHeliboards`: for `R.string.clipboard_provider_authority` and `R.string.gesture_data_provider_authority`, `assertFalse(value.startsWith("helium314.keyboard"))`
  - `imeNameIsGlasskey`: `assertEquals("Glasskey", context.getString(R.string.english_ime_name))` (default config; debug variant label is checked on device)
- [ ] **Step 2: Run** `./gradlew testDebugNoMinifyUnitTest --tests '*RebrandTest*'` — expect 3 FAIL.
- [ ] **Step 3: Apply the changes listed under Files.** Also grep `src/` for `"helium314.keyboard.clipprovider"`/`"helium314.keyboard.provider"` literals and route them through the string resources.
- [ ] **Step 4: Run the test** — PASS. Run the full unit suite `./gradlew testDebugNoMinifyUnitTest` and record the baseline failures that already exist upstream (CI uses the `runTests` variant because some tests are known to fail) in the commit message; no new failures allowed from here on.
- [ ] **Step 5: Device check** — `./gradlew assembleDebugNoMinify && adb install -r app/build/outputs/apk/debugNoMinify/*.apk`; `adb shell ime list -a -s` lists both `helium314.keyboard/...` and `io.github.r415uk3.glasskey.debug/...`.
- [ ] **Step 6: Commit** `feat: rebrand to Glasskey with own applicationId and authorities`

### Task 2: iOS theme style and color sets

**Files:**
- Modify: `app/src/main/java/helium314/keyboard/keyboard/KeyboardTheme.kt` (`STYLE_IOS = "iOS"`, add to `STYLES`; `THEME_ID_IOS_BASE = 5` + `KEYBOARD_THEMES` entry → `R.style.KeyboardTheme_iOS_Base`; branch in `getKeyboardTheme()` ignoring the borders pref; `THEME_IOS_LIGHT = "ios_light"`, `THEME_IOS_DARK = "ios_dark"` in `getAvailableDefaultColors` (light day-only, dark always) and `getThemeColors`)
- Modify: `app/src/main/res/values/attrs.xml:198` and `:351` — `<enum name="iOSBase" value="5"/>`
- Create: `app/src/main/res/values/themes-ios-base.xml` — full style family copied from `themes-rounded-base.xml` with `themeId=iOSBase`, the new drawables, `keyLetterSize 24sp`, `keyLabelSize 16sp`, `android:elevation 0dp` on the InputView style.
- Create drawables: `btn_keyboard_key_ios_base.xml` (selector incl. `state_active` → action variants, `state_pressed`), `btn_keyboard_key_normal_ios_base.xml` / `_pressed_` (white rounded rect 8.5dp, no bevel/stroke), `keyboard_popup_panel_background_ios_base.xml` (12dp radius), `btn_keyboard_key_popup_ios_base.xml`.
- Modify: `app/src/main/java/helium314/keyboard/latin/common/Colors.kt` — in `DefaultColors`, when `themeStyle == STYLE_IOS` use the bordered branch semantics (visible key, functional, spacebar each with own color; pressed = `brightenOrDarken`), regardless of `hasKeyBorders`; review every `STYLE_*` branch listed at `:77,180,295,428,441-468,481,494` and give iOS the Rounded behaviour unless stated here.
- Modify: `app/src/main/java/helium314/keyboard/keyboard/internal/KeyboardIconsSet.kt:24-28,295-299` — iOS maps to the new `ios` icon set (Task 4 fills it; until then alias Rounded).
- Modify: `app/src/main/res/values/strings.xml` — `style_name_iOS`, `theme_name_ios_light` ("iOS light"), `theme_name_ios_dark` ("iOS dark").
- Modify: `app/src/main/java/helium314/keyboard/latin/settings/Defaults.kt:47-54` — `PREF_THEME_STYLE = STYLE_IOS`, `PREF_ICON_STYLE = STYLE_IOS`, `PREF_THEME_COLORS = THEME_IOS_LIGHT`, `PREF_THEME_COLORS_NIGHT = THEME_IOS_DARK`, `PREF_THEME_DAY_NIGHT = true`.
- Test: `app/src/test/java/helium314/keyboard/IosThemeTest.kt`

**Interfaces:**
- Produces: `KeyboardTheme.STYLE_IOS: String`, `KeyboardTheme.THEME_IOS_LIGHT/THEME_IOS_DARK: String`, themeId `5`. Later tasks branch on `Settings.getValues().mThemeStyle == KeyboardTheme.STYLE_IOS` (or the equivalent field the codebase already exposes for the style — find it via `PREF_THEME_STYLE` readers; do not add a second field).

- [ ] **Step 1: Write the failing tests** (`@RunWith(RobolectricTestRunner)`, fresh prefs):
  - `defaultStyleIsIos`: `assertEquals(5, KeyboardTheme.getKeyboardTheme(ctx).themeId)`
  - `iosIgnoresBordersPref`: set `PREF_THEME_KEY_BORDERS=true` → still themeId 5
  - `darkColors`: force night (`SettingsActivity.forceNight = true`) → `getColorsForCurrentTheme(ctx)`: `get(ColorType.KEY_BACKGROUND) == 0x45FFFFFF`, `get(ColorType.FUNCTIONAL_KEY_BACKGROUND) == 0x45FFFFFF`, `get(ColorType.SPACE_BAR_BACKGROUND) == 0x45FFFFFF`, `get(ColorType.KEY_TEXT) == 0xFFFFFFFF.toInt()`, `get(ColorType.MAIN_BACKGROUND) == 0xDB1C1C1C.toInt()`
  - `lightColors`: `forceNight = false` → key `0xE3FFFFFF.toInt()`, text `0xA6000000.toInt()`, main background `0xBFDADADA.toInt()`
  - `actionKeyIsBlue`: `get(ColorType.ACTION_KEY_BACKGROUND) == 0xFF0088FF.toInt()` in both
  (Use the actual `ColorType` member names from `Colors.kt:624-669`; if a name differs, keep the assertion's meaning.)
- [ ] **Step 2: Run** `./gradlew testDebugNoMinifyUnitTest --tests '*IosThemeTest*'` — FAIL.
- [ ] **Step 3: Implement** per Files.
- [ ] **Step 4: Run** — PASS; full suite: no new failures.
- [ ] **Step 5: Commit** `feat: add iOS theme style with iOS light/dark colors as default`

### Task 3: dp-based geometry and the rounded panel

**Files:**
- Modify: `app/src/main/java/helium314/keyboard/keyboard/internal/KeyboardParams.java:211-265` (`readAttributes`) — when the theme is iOS, set `mRelativeHorizontalGap = 6.5dp / keyboardWidth`, `mVerticalGap = 10dp`, side padding `4dp` each, top padding `8dp`, bottom padding `0`, row height `45dp + 10dp`; key-gap scale pref still multiplies the gaps.
- Modify: `app/src/main/java/helium314/keyboard/latin/utils/ResourceUtils.java:71-96` (`getKeyboardHeight`) — iOS: base height = rows × 55dp + 8dp top (rows = 4, or 5 with number row), then the existing height-scale pref and the existing min/max clamp. When clamped by max, `KeyboardParams` scales row height and gaps proportionally (do not clip).
- Modify: `app/src/main/java/helium314/keyboard/latin/common/Colors.kt:539-549` (`setBackground(view, MAIN_BACKGROUND)`) — iOS: a `GradientDrawable` with color `MAIN_BACKGROUND` and corner radii `[27dp,27dp,27dp,27dp,0,0,0,0]` (top only; Android can't round to the screen's bottom corners reliably) plus a 1dp hairline stroke `#80FFFFFF` (light) / `#1AFFFFFF` (dark).
- Modify: `app/src/main/java/helium314/keyboard/latin/LatinIME.java:1654-1691` — iOS: the gesture-nav area under the keyboard gets the panel color (`MAIN_BACKGROUND`), so the panel reads as one piece down to the screen edge; `PREF_NAVBAR_COLOR` default stays true. (Translucent colors show the app behind only where the IME window is transparent; if that looks muddy on device, use the opaque equivalent and note it.)
- Modify: `Defaults.kt:96` — `PREF_BOTTOM_PADDING_SCALE` default `[0f,0f,0f,0f]` (the strip in Task 7 replaces it).
- Test: `app/src/test/java/helium314/keyboard/IosGeometryTest.kt`

**Interfaces:**
- Consumes: `STYLE_IOS` (Task 2).
- Produces: `KeyboardParams` fields as today; nothing new public.

- [ ] **Step 1: Write the failing tests** using `KeyboardParserTest`'s `buildKeyboard(EditorInfo(), subtype, KeyboardElement.ALPHABET)` pattern, but with a portrait size of 402dp × computed height at Robolectric's density:
  - `keyHeightIs45dp`: every non-spacer key in row 1 has `height == round(45dp)` (±1px)
  - `horizontalGapIs6_5dp`: x-distance between `q` and `w` key rects' facing edges == `round(6.5dp)` (±1px)
  - `rowGapIs10dp`: top of row 2 − bottom of row 1 == `round(10dp)` (±1px)
  - `landscapeShrinksNotClips` (Review Focus 3): with a height cap below 4×55dp, the sum of row heights + gaps ≤ keyboard height and every key height > 0
- [ ] **Step 2: Run** — FAIL.
- [ ] **Step 3: Implement** per Files.
- [ ] **Step 4: Run** — PASS; full suite clean.
- [ ] **Step 5: Device check** — install, open a notes app, `adb exec-out screencap -p > /tmp/…/t3-dark.png` with dark mode on, `adb shell cmd uimode night no` and repeat; compare against Figma node `934:8604` by eye (spacing, radius, top corners). Restore the user's night mode afterwards (`adb shell cmd uimode night` value read first).
- [ ] **Step 6: Commit** `feat: iOS key geometry in dp and rounded keyboard panel`

### Task 4: Font and icons

**Files:**
- Create fonts in `app/src/main/res/font/` — Regular + Medium static TTFs of each, all SIL OFL 1.1, license texts in `app/src/main/assets/licenses/<font>-OFL.txt`:
  - `inter_*` (github.com/rsms/inter 4.x), `golos_text_*` (Google Fonts "Golos Text"), `onest_*` (Google Fonts "Onest"), `noto_sans_*` (Google Fonts "Noto Sans").
  - A font is only bundled if it has glyphs for all of `ә ғ қ ң ө ұ ү һ і Ә Ғ Қ Ң Ө Ұ Ү Һ І ё ъ` — checked by the test below; one that fails is dropped from the list (note it in the commit), not patched.
- Create: `app/src/main/java/helium314/keyboard/latin/utils/KeyboardFont.kt` — `enum class KeyboardFont(val prefValue: String, @FontRes val regular: Int?, @FontRes val medium: Int?, @StringRes val title: Int) { SYSTEM("system", null, null, …), INTER("inter", …), GOLOS("golos", …), ONEST("onest", …), NOTO_SANS("noto_sans", …) }` + `fun fromPref(value: String?): KeyboardFont` (unknown → `INTER`).
- Modify: `Settings.java` / `Defaults.kt` — new pref `PREF_KEYBOARD_FONT = "keyboard_font"`, default `"inter"` (applies to every style, not only iOS).
- Modify: `app/src/main/java/helium314/keyboard/latin/utils/KeyboardTypeface.kt:24-28` — order: user custom font file (existing HeliBoard feature) → selected `KeyboardFont` (`SYSTEM` = today's behaviour, i.e. the key's own default typeface) → default. Multi-char functional labels (`123`, `ABC`, `.com`, `#+=`) use the font's `medium`. `drawKeyPopupHint` (`KeyboardView.java:561`) also goes through `KeyboardTypeface.resolve`. Changing the pref calls `KeyboardTypeface.clearCache()` and reloads the keyboard.
- Modify: `app/src/main/java/helium314/keyboard/settings/screens/AppearanceScreen.kt:~88` — a list preference "Шрифт / Font" right above the existing custom-font entry, items rendered each in its own font, values from `KeyboardFont.entries`. Strings in `values/strings.xml` (en) and `values-ru/strings.xml` (ru).
- Create vector drawables (24dp viewport, 1.6–1.8dp stroke, rounded caps, drawn fresh — SF-Symbols-like, not copied): `ic_ios_shift.xml`, `ic_ios_shift_filled.xml` (shifted), `ic_ios_caps_lock.xml` (filled + underline), `ic_ios_delete.xml`, `ic_ios_return.xml`, `ic_ios_emoji.xml`, `ic_ios_globe.xml`, `ic_ios_mic.xml`, `ic_ios_go.xml` (→), `ic_ios_search.xml`.
- Modify: `KeyboardIconsSet.kt` — `ios` set mapping shift/shift_shifted/caps_lock/delete/enter/emoji/language_switch/search/go(next) to the above; anything unmapped falls back to Rounded.
- Modify: `TextKeyData.kt:442,446` / `KeyboardTheme.getThemeActionAndEmojiKeyLabelFlags` — iOS: action key shows an icon (return arrow / go arrow / search), never a text label like "Go".
- Test: `app/src/test/java/helium314/keyboard/IosFontIconTest.kt`

**Interfaces:**
- Consumes: `STYLE_IOS`.
- Produces: drawable names above; `R.drawable.ic_ios_globe` and `R.drawable.ic_ios_mic` are used by Task 7.

- [ ] **Step 1: Failing tests**:
  - `defaultFontIsInter`: fresh prefs → `KeyboardFont.fromPref(prefs.getString(PREF_KEYBOARD_FONT, …)) == INTER` and `KeyboardTypeface.resolve("a", Typeface.DEFAULT)` != `Typeface.DEFAULT`
  - `systemFontKeepsDefault`: pref `"system"` → `resolve("a", Typeface.DEFAULT) === Typeface.DEFAULT`
  - `unknownPrefFallsBackToInter`: `fromPref("comic") == INTER`
  - `everyBundledFontCoversKazakh`: for each entry with `regular != null`, `Paint().apply { typeface = ResourcesCompat.getFont(ctx, regular) }.hasGlyph(c)` is true for every char in the Kazakh/Russian set above (Robolectric's Paint may not implement `hasGlyph` — if so, run this one as an instrumented test in `app/src/androidTest/` on the phone via `./gradlew connectedDebugNoMinifyAndroidTest`)
  - `customFontStillWins`: with a custom font file present (copy any bundled TTF to `Settings.getCustomFontFile`), resolve returns that file's typeface, not the selected `KeyboardFont`'s
  - `iosIconsResolve`: `KeyboardIconsSet` for style iOS returns non-null drawables for `shift`, `delete`, `enter`, `emoji`, `language_switch`
- [ ] **Step 2: Run** — FAIL. **Step 3: Implement.** **Step 4: Run** — PASS, suite clean.
- [ ] **Step 5: Commit** `feat: selectable keyboard font (Inter default) and iOS-style icons`

### Task 5: iOS layouts (bottom row variants, symbols, numpad)

**Files:**
- Create: `app/src/main/assets/layouts/functional/ios_functional.json` (name must NOT start with `functional_keys`, because `LayoutUtils` picks defaults by prefix):
  - row above placeholder: `shift` width 0.12 | spacer 0.02 | placeholder | spacer 0.02 | `delete` width 0.12
  - bottom row by `variation_selector`:
    - default/normal/password: `symbol_alpha` 0.225 | `emoji` 0.11 | `space` (-1) | `action` 0.225
    - search (use the `variation_selector` option that maps to IME_ACTION_SEARCH; if none, `keyboard_state_selector`/action-based selection — add one option in `KeyData.kt` only if necessary): `symbol_alpha` | `emoji` | `space` | `period` 0.11 | `action`
    - email: `symbol_alpha` | `emoji` | `space` | `@` 0.11 | `period` 0.11 | `action`
    - uri: `symbol_alpha` | `emoji` | `period` | `/` | `.com` (text key, label ".com", width 0.16) | `action` — no space key, exactly as Figma
  - emoji key is unconditional (not behind `emojiKeyEnabled`); no `language_switch`, no `comma`.
- Create: `app/src/main/assets/layouts/symbols/ios_symbols.txt` — rows: `1 2 3 4 5 6 7 8 9 0` / `- / : ; ( ) ₸ & @ "` / `. , ? ! '` (₸ replaces €; € and $ go in ₸'s popups).
- Create: `app/src/main/assets/layouts/more_symbols/ios_symbols_shifted.txt` — `[ ] { } # % ^ * + =` / `_ \ | ~ < > € $ £ •` / `. , ? ! '`.
- Create: `app/src/main/assets/layouts/phone/ios_phone.json` and `app/src/main/assets/layouts/number/ios_number.json` — 3×4: `1` `2 ABC` `3 DEF` / `4 GHI` `5 JKL` `6 MNO` / `7 PQRS` `8 TUV` `9 WXYZ` / phone: `+*#` | `0` (popup `+`) | `delete`; number: `.`/`,` (locale decimal) | `0` | `delete`. Letters render as a hint-row under the digit (reuse the existing phone layout's hint-label mechanism; size 10sp bold).
- Modify: `Defaults.kt:30-43` (`LayoutType` defaults) — iOS style → FUNCTIONAL `ios_functional` (also for tablets), SYMBOLS `ios_symbols`, MORE_SYMBOLS `ios_symbols_shifted`, PHONE `ios_phone`, NUMBER `ios_number`. Non-iOS styles keep the stock defaults.
- Modify: `LocaleKeyboardInfos.kt:22-28` defaults when iOS: `symbol` label `123`, `shift_symbol` label `#+=`, alphabet label `ABC`; `assets/locale_key_texts/ru.txt` and `kk.txt` keep `alphabet: АБВ`.
- Modify: `PopupKeysUtils.kt:17-22` default order for iOS → `language_priority:true|language:true|layout:true|number:false|symbols:false`; hint order all false (iOS shows no hint glyphs on keys).
- Modify: `KeyboardParser.kt:273-278` — `numberRowInSymbols` must not inject a second number row into `ios_symbols` (it already has 1–0): default `PREF_SHOW_NUMBER_ROW_IN_SYMBOLS=false` for iOS.
- Test: `app/src/test/java/helium314/keyboard/IosLayoutTest.kt` (copy setup from `KeyboardParserTest.kt:42-58`, reuse its `buildKeyboard` by moving it to a shared `test/.../KeyboardTestUtils.kt` if it is private)

**Interfaces:**
- Consumes: `STYLE_IOS`.
- Produces: layout names `ios_functional`, `ios_symbols`, `ios_symbols_shifted`, `ios_phone`, `ios_number`.

- [ ] **Step 1: Failing tests** — helper `rowCodes(element, editorInfo, locale, mainLayout): List<List<Int>>` dropping spacers; assert on `mCode` for functional keys and `mLabel` for letters:
  - `enBottomRow`: last row codes == `[SYMBOL_ALPHA, EMOJI, ' '.code, ACTION_ENTER]` (use the actual `KeyCode` constants)
  - `ruBottomRowSameAsEn`: Russian subtype, same bottom row; row 1 starts `й ц у к е н`
  - `kkPopups`: kk subtype, key `а` popup contains `ә`; key `к` contains `қ`; no digit in the popups
  - `emailRow`: `EditorInfo.inputType = TYPE_CLASS_TEXT or TYPE_TEXT_VARIATION_EMAIL_ADDRESS` → bottom row `[SYMBOL_ALPHA, EMOJI, ' ', '@', '.', ACTION_ENTER]`
  - `urlRow`: `TYPE_TEXT_VARIATION_URI` → `[SYMBOL_ALPHA, EMOJI, '.', '/', <.com text key>, ACTION_ENTER]` and no space key
  - `searchRow`: `imeOptions = IME_ACTION_SEARCH` → contains `'.'` before action
  - `symbolsPage`: SYMBOLS row 1 labels `1..0`, row 2 contains `₸`, bottom functional row has `symbol_alpha` labelled `ABC`/`АБВ`
  - `moreSymbolsToggleLabel`: the shift key on SYMBOLS is labelled `#+=`
  - `phoneField`: `TYPE_CLASS_PHONE` → PHONE element, 4 rows × 3 keys, row 1 labels `1 2 3`
  - `noShiftOrLanguageKeyOnNumpad` (Review Focus 1): PHONE/NUMBER rows contain no `LANGUAGE_SWITCH` / `SHIFT`
  - `parseExistingLayouts` (existing test) still passes and covers the new files.
- [ ] **Step 2: Run** — FAIL. **Step 3: Implement.** **Step 4: Run** — PASS, suite clean.
- [ ] **Step 5: Device check** — screenshots of: notes (text), Chrome address bar (URL), a login email field, dialer search or Contacts phone field; compare with Figma `936:3190`, `937:5701`, `1806:3400`.
- [ ] **Step 6: Commit** `feat: iOS layouts for letters, symbols, numpad and input-type variants`

### Task 6: Out-of-the-box defaults and Kazakh dictionary

**Files:**
- Modify: `Defaults.kt` —
  - `PREF_ENABLED_SUBTYPES` = ru (russian), en-US (qwerty), kk (russian) in the `languageTag§extraValues` format (`SettingsSubtype.toPref`), each matching `mainLayoutNameOrQwerty()` of the `method.xml` subtype, else `loadEnabledSubtypes` drops it; `PREF_SELECTED_SUBTYPE` = ru.
  - `PREF_TOOLBAR_MODE = "SUGGESTION_STRIP"`, `PREF_SHOW_LANGUAGE_SWITCH_KEY = false`, `PREF_SHOW_EMOJI_KEY = false` (emoji is unconditional in the iOS layout), `PREF_SPACE_TO_CHANGE_LANG = false`, `PREF_SPACE_HORIZONTAL_SWIPE = MOVE_CURSOR` (unchanged), `PREF_SHOW_HINTS = false`, `PREF_SHOW_POPUP_HINTS = false`, `PREF_VIBRATE_ON = true`, `PREF_SOUND_ON = true` (system volume via `-0.01f` as today), `PREF_POPUP_ON = true` (and make `initDynamicDefaults` not turn it off on phones), `PREF_SHOW_SETUP_WIZARD_ICON = false`, `PREF_ENABLE_CLIPBOARD_HISTORY = false`.
  - Keep: autocap, double-space period, autocorrect, backspace-reverts-autocorrect, gesture input (only active once the user loads the library).
- Create: `app/src/main/assets/dicts/main_kk.dict` — the file from `https://codeberg.org/Helium314/aosp-dictionaries/raw/branch/main/dictionaries/main_kk__experimental.dict` (11.3 MB); add its source + license line to the repo's dictionary credits (find where bundled dicts are credited; if nowhere, add to `README.md`).
- Test: `app/src/test/java/helium314/keyboard/IosDefaultsTest.kt`

**Interfaces:**
- Consumes: layouts from Task 5.

- [ ] **Step 1: Failing tests** (fresh prefs):
  - `enabledSubtypesAreRuEnKk`: `SubtypeSettings.getEnabledSubtypes(...)` locales == setOf(`ru`, `en-US`, `kk`); kk/ru main layout `russian`, en `qwerty`
  - `selectedIsRussian`
  - `toolbarModeIsSuggestionStrip`: `Settings.getValues().mToolbarMode == ToolbarMode.SUGGESTION_STRIP` after `Settings.loadSettings`
  - `kkDictionaryIsBundled`: `DictionaryInfoUtils.getAssetsDictionaryList(ctx)` contains `main_kk.dict`
- [ ] **Step 2: Run** — FAIL. **Step 3: Implement.** **Step 4: Run** — PASS, suite clean.
- [ ] **Step 5: Device check** — `adb shell pm clear io.github.r415uk3.glasskey.debug`, enable + switch to it (`adb shell ime enable …; ime set …`), type "сәлем" → suggestions appear; globe-less switching still possible through the system picker for now.
- [ ] **Step 6: Commit** `feat: iOS defaults — ru/en/kk enabled, no toolbar, bundled kk dictionary`

### Task 7: Bottom strip with globe (mic slot reserved)

**Files:**
- Create: `app/src/main/res/layout/ios_bottom_bar.xml` — horizontal bar, height `56dp` (iOS's 80pt includes the home indicator; Android's gesture area is added by insets on top of this), globe `ImageButton` (`@id/ios_globe`, 44×44dp touch, 27dp icon, start margin 25dp, centered vertically) and mic `ImageButton` (`@id/ios_mic`, end margin 25dp, `visibility=gone` — Plan 2).
- Create: `app/src/main/java/helium314/keyboard/latin/ios/IosBottomBar.kt` — `class IosBottomBar(context, attrs) : LinearLayout`; `fun bind(listener: KeyboardActionListener, onLongPressGlobe: () -> Unit)`; `fun updateColors(colors: Colors)` (icon tint = `KEY_TEXT`); applies `WindowInsetsCompat.Type.navigationBars()` bottom as bottom padding via `ViewCompat.setOnApplyWindowInsetsListener`.
- Create: `app/src/main/java/helium314/keyboard/latin/ios/GlobeController.kt` — `class GlobeController(private val onCode: (Int) -> Unit, private val showPicker: () -> Unit) { fun onTap(); fun onLongPress() }`; tap → `onCode(KeyCode.LANGUAGE_SWITCH)`; long-press → `showPicker()` and performs `HapticFeedbackConstants.LONG_PRESS` through the view.
- Modify: `app/src/main/res/layout/main_keyboard_frame.xml:~77` — include `ios_bottom_bar` after `keyboard_view_wrapper`, `visibility=gone` by default.
- Modify: `app/src/main/java/helium314/keyboard/keyboard/KeyboardSwitcher.java:549-569` — find it, show it only for iOS style and non-floating keyboard, bind it (`listener` = the same `KeyboardActionListener` the main view uses; long-press → `latinIME.showInputPickerDialog()`), call `updateColors`.
- Modify: `app/src/main/java/helium314/keyboard/latin/LatinIME.java:1109-1160` (`onComputeInsets`) — include the bar's height in `visibleTopY` (`inputHeight - wrapper - strip - bar`), touchable region unchanged otherwise.
- Modify: `LatinIME.showInputPickerDialog()` (`:1285-1293`) — iOS: show the picker even if there is only one IME as long as there are ≥2 subtypes (it currently bails out when "only one"; verify and adjust only if it hides with 3 subtypes).
- Test: `app/src/test/java/helium314/keyboard/latin/ios/GlobeControllerTest.kt`, plus one case in `InputLogicTest`-style harness.

**Interfaces:**
- Consumes: `R.drawable.ic_ios_globe`, `ic_ios_mic` (Task 4), `Colors`.
- Produces: `IosBottomBar` with `@id/ios_mic` reserved for Plan 2; `GlobeController(onCode, showPicker)`.

- [ ] **Step 1: Failing tests**:
  - `GlobeControllerTest.tapSendsLanguageSwitch`: recorded codes == `[KeyCode.LANGUAGE_SWITCH]`
  - `GlobeControllerTest.longPressOpensPicker`: picker callback count == 1, no code sent
  - Review Focus 2, in a Robolectric `LatinIME` harness like `InputLogicTest` (`Robolectric.setupService(LatinIME::class.java)`) with the Task 6 defaults: from `ru` while on the SYMBOLS page and shifted, `latinIME.onCodeInput(KeyCode.LANGUAGE_SWITCH, …)` → current subtype locale is the next one in enabled order and the keyboard element is ALPHABET (use the `KeyboardSwitcher` shadow's state or `mKeyboardSwitcher.getKeyboard().mId.mElementId` — whichever the harness exposes).
- [ ] **Step 2: Run** — FAIL. **Step 3: Implement.** **Step 4: Run** — PASS, suite clean.
- [ ] **Step 5: Device check** — gesture handle not overlapping the globe; tap cycles ru → en → kk → ru (order as enabled); long-press opens the list; app content above the keyboard is not hidden behind it (open a chat-like app, last message visible above the strip). Screenshot light+dark.
- [ ] **Step 6: Commit** `feat: iOS bottom strip with globe language switch`

### Task 8: iOS suggestion strip

**Files:**
- Modify: `app/src/main/res/values/themes-ios-base.xml` — `SuggestionStripView.iOS_Base` / `SuggestionWord.iOS_Base`: text size 18sp, no bold for the center word (iOS shows it regular; auto-correct candidate gets the subtle key-color pill instead, see below), divider drawable `suggestions_strip_divider_ios.xml` (1×24dp, color via `Colors` `STRIP_DIVIDER` or tint — dark `#4DFFFFFF`, light `#1A000000`).
- Modify: `SuggestionStripView.kt:513-528` (`updateKeys`) — iOS: `toolbarExpandKey` and `pinnedKeys` always `GONE`, including incognito.
- Modify: `SuggestionStripLayoutHelper.java` — iOS: the word that will be auto-committed on space gets a rounded 8.5dp background in `KEY_BACKGROUND` color (iOS highlights it), instead of HeliBoard's bold/colored text.
- Modify: `app/src/main/res/values/config.xml:54` is global; for iOS override `config_suggestions_strip_height` via the style or code to 44dp.
- Test: `app/src/test/java/helium314/keyboard/latin/IosStripTest.kt` (Robolectric; inflate `R.layout.suggestions_strip` into a themed context `ContextThemeWrapper(ctx, R.style.KeyboardTheme_iOS_Base)` if feasible; otherwise test the pure decision function below)

**Interfaces:**
- Produces: `fun shouldShowStripExtras(isIos: Boolean, mode: ToolbarMode, incognito: Boolean): Boolean` in `SuggestionStripView.kt` companion (pure, tested), used by `updateKeys`.

- [ ] **Step 1: Failing tests** — `shouldShowStripExtras(true, SUGGESTION_STRIP, incognito = true) == false` (Review Focus 5); `(true, SUGGESTION_STRIP, false) == false`; `(false, EXPANDABLE, false) == true` (stock behaviour preserved).
- [ ] **Step 2: Run** — FAIL. **Step 3: Implement.** **Step 4: Run** — PASS, suite clean.
- [ ] **Step 5: Device check** — type "привт" → middle slot shows "привет" with the pill; password field (any login form) → no expand key; compare with Figma `212:365`.
- [ ] **Step 6: Commit** `feat: iOS suggestion strip`

### Task 9: iOS key preview balloon and end-to-end check

**Files:**
- Create: `app/src/main/res/drawable/keyboard_key_feedback_ios.xml` (+ left/right-edge variants via the same custom states as `keyboard_key_feedback_lxx_light`) — balloon: rounded top 12dp, narrows to the key width at the bottom so it visually merges with the pressed key; white, tinted by `KEY_PREVIEW_BACKGROUND` (set that color to the key color made opaque: light `#FFFFFFFF`, dark `#FF6B6B6B`).
- Modify: `themes-ios-base.xml` — `keyPreviewBackground`, `keyPreviewOffset` so the balloon's bottom overlaps the key; preview text ratio so the letter is ~1.6× key letter size.
- Modify: `KeyPreviewChoreographer.java:83-120` — iOS: balloon width = key width + 2×12dp, height = key height × 2.1; on the top row it must not be clipped by the strip (it may overlap the suggestion strip, which iOS does too).
- Test: none automatable beyond what exists; this task is verified on device.

- [ ] **Step 1: Implement.**
- [ ] **Step 2: Device check** — record `adb shell screenrecord --time-limit 8 /sdcard/t9.mp4` while typing "qwerty asdf" in light and dark; pull and inspect frames: balloon on `q` and `p` edges not clipped, top row not clipped.
- [ ] **Step 3: Full unit suite** — no new failures vs. the Task 1 baseline.
- [ ] **Step 4: End-to-end on the phone** — install release-like `assembleRelease` signed with the debug key (or `debugNoMinify`), set as default IME, walk through: three languages via globe; email/URL/search/phone fields; shift/caps; symbols ↔ #+=; autocorrect revert with delete; dark ↔ light. Hand the phone to the user for a 10-minute try and collect what bothers them.
- [ ] **Step 5: Commit** `feat: iOS key preview balloon`
