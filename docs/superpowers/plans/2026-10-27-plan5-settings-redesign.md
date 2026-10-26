# Plan 5 — iOS 27 settings with Liquid Glass

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** replace HeliBoard's Material settings with a short iOS 27-style settings app with Liquid Glass (bars, search, groups, switch thumbs).

**Architecture:** a new `settings/ios/` package: glass + iOS components (Compose), pure logic objects with unit tests, and new screens wired through a new `IosSettingsNavHost` that `SettingsActivity` uses instead of `SettingsNavHost`. Old HeliBoard screens stay in the code (upstream-merge friendly) but are unreachable except `DictionaryScreen`, which «Словари» opens. Glass uses Kyant's `backdrop` library: the page background (and, for the top bar, the content) is recorded as a backdrop and glass shapes draw it with blur + lens.

**Tech Stack:** Kotlin, Jetpack Compose (BOM 2025.11.01), `io.github.kyant0:backdrop:1.0.x` (Apache-2.0), `sh.calvin.reorderable` (already a dependency), Robolectric + `androidx.compose.ui:ui-test-junit4` for screen tests.

**Spec:** `docs/superpowers/specs/2026-10-27-settings-redesign-design.md` (mockups `.superpowers/brainstorm/49827-1793051131/content/settings-v2.html`, `settings-glass-v3.html`).

## Global Constraints

- minSdk stays 21, compose BOM stays 2025.11.01: pick the newest `backdrop` 1.0.x whose `foundation` dependency is not newer than the BOM's (1.0.0 needs foundation 1.9.4 — verify with the POM before adding). Note it in `docs/third-party.md`.
- Glass levels: real lens refraction on Android 13+ (API 33), blur only on Android 12 (31–32), translucent fill below 31.
- Settings UI follows system light/dark; strings in `values`, `values-ru`, `values-kk` (new file `strings-ios-settings.xml` each).
- Kept settings and their prefs exactly: Автопрописные `PREF_AUTO_CAP`, Автокоррекция `PREF_AUTO_CORRECTION`, Предиктивный набор `PREF_SHOW_SUGGESTIONS`, Просмотр символов `PREF_POPUP_ON`, Быстрая клавиша «.» `PREF_KEY_USE_DOUBLE_SPACE_PERIOD` (footer «Двойное нажатие пробела вставляет точку и пробел.»), Звук `PREF_SOUND_ON`, Вибрация `PREF_VIBRATE_ON`, Шрифт `PREF_KEYBOARD_FONT`, Ряд цифр `PREF_SHOW_NUMBER_ROW`, Тема (`PREF_THEME_DAY_NIGHT` + `PREF_THEME_COLORS`), Размытие фона (new `PREF_IOS_BLUR`, default true).
- iOS metrics (dp): group corner 26, row min height 46, row side padding 16, separator 0.5 inset 16, section header 13sp grey 7dp above group, footer 13sp grey, large title 32sp bold, switch 52×31 (thumb 38×27 at rest, 46×35 glass while pressed), iOS green `#34C759`, tint blue `#0A84FF`, glass back button 44×44 circle, bottom search field 48 high radius 24.
- Hidden settings keep their values; nothing writes defaults on upgrade.
- Source link in «О Glasskey»: `https://github.com/r415uk3/glasskey`.

## Review Focus

1. Removing the last enabled keyboard language must be impossible (row not deletable, or removal refused) — the keyboard would have no language.
2. Reordering languages while one of them is the current keyboard: the current language stays selected, and the globe follows the new order right away.
3. Android 12 and older (no RuntimeShader / no blur): settings open without crash and glass degrades to blur / fill.
4. Settings search must never surface hidden HeliBoard settings.
5. Turning «Размытие фона» off makes the keyboard panel opaque the next time the keyboard opens (no translucent panel without blur).

---

### Task 1: Glass foundation and test setup

**Files:**
- Modify: `app/build.gradle.kts` (backdrop; `testImplementation("androidx.compose.ui:ui-test-junit4")`, `debugImplementation`/`"debugNoMinifyImplementation"("androidx.compose.ui:ui-test-manifest")`), `docs/third-party.md`
- Create: `settings/ios/IosGlass.kt`, `settings/ios/IosSettingsTheme.kt`, `settings/ios/IosBackground.kt`
- Test: `app/src/test/java/helium314/keyboard/settings/ios/IosGlassTest.kt`

**Interfaces — Produces:**
- `enum class GlassLevel { LENS, BLUR, FILL }`; `object IosGlass { fun levelFor(sdk: Int): GlassLevel }`
- `@Composable fun rememberIosBackdrop(): <backdrop type of the library>`; `fun Modifier.iosBackdropSource(backdrop)`; `fun Modifier.iosGlass(backdrop, shape: Shape, level: GlassLevel = IosGlass.levelFor(Build.VERSION.SDK_INT)): Modifier` — LENS: blur 2dp + lens (refraction height ~12dp, amount ~24dp) + top highlight; BLUR: blur 20dp + fill; FILL: `rgba(255,255,255,.08)` dark / `rgba(255,255,255,.55)` light + 0.5dp light border.
- `data class IosSettingsColors(background, groupFill, text, secondaryText, separator, tint, switchOn, switchOff)`; `object IosSettingsTheme { fun colors(dark: Boolean): IosSettingsColors }`; `@Composable fun IosSettingsTheme(content)` providing `LocalIosColors`.
- `@Composable fun IosBackground(dark: Boolean, modifier)` — blurred blobs: dark `#1F4FBF`, `#7B2FF7`, `#0F8F7F` on `#05060A`; light: the same hues at 25% on `#F2F2F7`.

- [ ] **Step 1: Write failing tests**

```kotlin
@Test fun lensOnAndroid13() = assertEquals(GlassLevel.LENS, IosGlass.levelFor(33))
@Test fun blurOnAndroid12() { assertEquals(GlassLevel.BLUR, IosGlass.levelFor(31)); assertEquals(GlassLevel.BLUR, IosGlass.levelFor(32)) }
@Test fun fillBelow12() = assertEquals(GlassLevel.FILL, IosGlass.levelFor(30))
@Test fun darkAndLightColors() {
    assertEquals(Color(0xFF34C759), IosSettingsTheme.colors(dark = true).switchOn)
    assertEquals(Color(0xFF0A84FF), IosSettingsTheme.colors(dark = false).tint)
    assertEquals(Color.White, IosSettingsTheme.colors(dark = true).text)
    assertEquals(Color.Black, IosSettingsTheme.colors(dark = false).text)
}
```

- [ ] **Step 2:** Run `./gradlew :app:testDebugUnitTest --tests 'helium314.keyboard.settings.ios.IosGlassTest'` → FAIL (unresolved).
- [ ] **Step 3:** Add dependencies, implement the three files. Check the library's README for the exact 1.0.x API names (`rememberLayerBackdrop`, `layerBackdrop`, `drawBackdrop`, `blur`, `lens`, `vibrancy`) and wrap them so no other file imports the library.
- [ ] **Step 4:** Tests PASS; `./gradlew :app:assembleDebugNoMinify` succeeds.
- [ ] **Step 5:** Commit `feat: iOS settings glass foundation`.

### Task 2: iOS components

**Files:** Create `settings/ios/IosScreen.kt`, `IosList.kt` (groups, rows, header, footer), `IosSwitch.kt`, `IosSearchField.kt`, `IosLogic.kt`; Test `settings/ios/IosLogicTest.kt`, `settings/ios/IosComponentsTest.kt` (Robolectric + compose rule).

**Interfaces:**
- Consumes: Task 1.
- Produces:
  - `object IosLargeTitle { fun collapse(scrollPx: Int, titleHeightPx: Int): Float }` — 0 at rest, 1 when the large title has scrolled by its own height, clamped.
  - `object IosSwitchLogic { fun checkedAfterRelease(startChecked: Boolean, dragFraction: Float, moved: Boolean): Boolean }` — tap toggles; drag decides by side (`dragFraction` 0 = off end, 1 = on end, 0.5 threshold).
  - `@Composable fun IosScreen(title: String, onBack: (() -> Unit)?, bottomBar: @Composable (() -> Unit)? = null, content: LazyListScope.() -> Unit)` — background + backdrop source, large title on every screen (as in iOS 27 Settings sub-pages too), glass circle back button top-left when `onBack != null`, glass top bar fades in with `collapse`.
  - `fun LazyListScope.iosGroup(header: String? = null, footer: String? = null, rows: List<@Composable () -> Unit>)`; `@Composable fun IosRow(title: String, value: String? = null, icon: IosRowIcon? = null, chevron: Boolean = onClick != null, titleColor: Color? = null, onClick: (() -> Unit)? = null, trailing: (@Composable () -> Unit)? = null)`; `@Composable fun IosSwitchRow(title: String, prefKey: String, default: Boolean)`; `@Composable fun IosCheckRow(title: String, checked: Boolean, onClick: () -> Unit)`.
  - `@Composable fun IosSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit)` — pill; while pressed the thumb grows to 46×35 and is drawn with `iosGlass(level)`.
  - `@Composable fun IosSearchField(query: String, onQueryChange: (String) -> Unit, modifier)` — glass, floating.

- [ ] **Step 1: Failing tests**

```kotlin
// IosLogicTest
@Test fun titleCollapse() {
    assertEquals(0f, IosLargeTitle.collapse(0, 100)); assertEquals(0.5f, IosLargeTitle.collapse(50, 100))
    assertEquals(1f, IosLargeTitle.collapse(400, 100)); assertEquals(0f, IosLargeTitle.collapse(-20, 100))
}
@Test fun switchTapToggles() = assertEquals(true, IosSwitchLogic.checkedAfterRelease(false, 0f, moved = false))
@Test fun switchDragDecidesBySide() {
    assertEquals(true, IosSwitchLogic.checkedAfterRelease(false, 0.8f, moved = true))
    assertEquals(false, IosSwitchLogic.checkedAfterRelease(true, 0.3f, moved = true))
}
// IosComponentsTest (createComposeRule, Robolectric)
@Test fun switchRowWritesPref() { /* IosSwitchRow("Звук", PREF_SOUND_ON, false); click node "Звук" → prefs PREF_SOUND_ON == true */ }
@Test fun rowWithClickShowsChevronAndCallsBack() { /* IosRow("Клавиатуры", value = "3", onClick = …) → text "3" displayed, click → callback once */ }
```

- [ ] **Step 2:** Run both test classes → FAIL.
- [ ] **Step 3:** Implement. Rows: whole row clickable (switch rows toggle on row tap too), pressed row highlight `separator` colour, no Material ripple.
- [ ] **Step 4:** Tests PASS.
- [ ] **Step 5:** Commit `feat: iOS settings components`.

### Task 3: Data behind the new screens

**Files:**
- Modify: `latin/utils/SubtypeSettings.kt` (keep enabled order: no `sortBy` at :65, `createPrefSubtypes` keeps order with `distinct()` instead of `toSortedSet()`; add `fun moveEnabledSubtype(context: Context, from: Int, to: Int)` that rewrites `PREF_ENABLED_SUBTYPES` and the in-memory list and calls `RichInputMethodManager.getInstance().refreshSubtypeCaches()`), `latin/settings/Settings.java`/`Defaults.kt` (`PREF_IOS_BLUR = "ios_blur"`, default `true`), `latin/ios/IosBlur.kt` (`isAvailable` also requires the pref), `settings/SettingsContainer.kt` (`fun filter(searchTerm: String, allowedKeys: Set<String>): List<Setting>`)
- Create: `settings/ios/IosThemeChoice.kt`, `settings/ios/IosTextReplacements.kt`
- Test: `IosSettingsDataTest.kt` (Robolectric), extend `IosGlobeSwitchTest.kt`

**Interfaces — Produces:**
- `enum class IosThemeChoice { SYSTEM, LIGHT, DARK; companion object { fun read(prefs): IosThemeChoice; fun write(prefs, choice) } }` — SYSTEM: day/night on; LIGHT: day/night off + colors `THEME_IOS_LIGHT`; DARK: day/night off + colors `THEME_IOS_DARK`.
- `data class TextReplacement(val word: String, val shortcut: String?, val locale: Locale?)`; `object IosTextReplacements { fun all(context): List<TextReplacement>; fun add(context, word, shortcut); fun delete(context, r: TextReplacement) }` — all locales, new entries with `locale = null` ("all languages"); reuse PersonalDictionaryScreen's content-provider helpers (make them `internal`).
- `SettingsContainer.filter(term, allowedKeys)`; `val IOS_SEARCHABLE_KEYS: Set<String>` in `settings/ios/IosSettingsKeys.kt` = the kept pref keys of Global Constraints.

- [ ] **Step 1: Failing tests**

```kotlin
@Test fun enabledOrderIsKeptAndMovable() {
    // enable ru, en-US, kk in this order -> getEnabledSubtypes() locales == [ru, en-US, kk]
    SubtypeSettings.moveEnabledSubtype(context, 2, 0)            // kk to top
    assertEquals(listOf("kk", "ru", "en-US"), SubtypeSettings.getEnabledSubtypes().map { it.locale().toLanguageTag() })
    SubtypeSettings.reloadEnabledSubtypes(context)               // survives reload from prefs
    assertEquals(listOf("kk", "ru", "en-US"), SubtypeSettings.getEnabledSubtypes().map { it.locale().toLanguageTag() })
}
@Test fun globeFollowsUserOrder() { /* after the move, current = kk -> getNextSubtypeInThisIme(true) is ru */ }
@Test fun themeChoiceRoundTrip() { for (c in IosThemeChoice.entries) { IosThemeChoice.write(prefs, c); assertEquals(c, IosThemeChoice.read(prefs)) } }
@Test fun darkChoiceUsesIosDarkColors() { IosThemeChoice.write(prefs, IosThemeChoice.DARK); assertEquals(false, prefs.getBoolean(PREF_THEME_DAY_NIGHT, true)); assertEquals(KeyboardTheme.THEME_IOS_DARK, prefs.getString(PREF_THEME_COLORS, null)) }
@Test fun blurOffMeansNoPanelBlur() { prefs.edit { putBoolean(PREF_IOS_BLUR, false) }; assertEquals(false, IosBlur.isAvailable(context)) }
@Test fun searchFindsOnlyKeptSettings() {
    val c = SettingsContainer(context)
    assertTrue(c.filter("auto", IOS_SEARCHABLE_KEYS).all { it.key in IOS_SEARCHABLE_KEYS })
    assertTrue(c.filter("toolbar", IOS_SEARCHABLE_KEYS).isEmpty())
}
```
(Text replacements: Robolectric's `UserDictionary` provider is not available — cover `IosTextReplacements` by a device check in Task 6 instead.)

- [ ] **Step 2:** Run → FAIL.
- [ ] **Step 3:** Implement. Existing users keep their current (alphabetical) order until they reorder.
- [ ] **Step 4:** Tests PASS, and the existing `IosGlobeSwitchTest`, `IosDefaultsTest`, `SubtypeTest`-style tests still pass.
- [ ] **Step 5:** Commit `feat: user-ordered languages, theme choice, blur switch, iOS search scope`.

### Task 4: Main screen, navigation, enable card

**Files:** Create `settings/ios/IosSettingsNavHost.kt`, `settings/ios/screens/IosMainScreen.kt`; Modify `settings/SettingsActivity.kt` (use `IosSettingsNavHost`; no automatic `WelcomeWizard` — the card opens it); strings.
Test: `settings/ios/IosMainScreenTest.kt` (compose rule).

**Interfaces — Produces:** routes `IosRoute.Main, Keyboards, AddKeyboard, Layout + "{subtype}", Dictionaries, TextReplacement, Appearance, About`; `@Composable fun IosMainScreen(needsEnabling: Boolean, onEnable: () -> Unit, navigate: (String) -> Unit)`.

- [ ] **Step 1: Failing tests** — `mainScreenShowsOnlyKeptItems`: texts «Клавиатуры», «Замена текста», «Оформление», «Автопрописные», «Автокоррекция», «Предиктивный набор», «Просмотр символов», «Быстрая клавиша «.»», «Звук», «Вибрация», «О Glasskey» exist (ru locale config); «Toolbar»/«Панель инструментов», «Advanced»/«Дополнительно» do not. `enableCardOnlyWhenNeeded`: card text «Включить Glasskey» shown for `needsEnabling = true`, absent for false. `searchShowsKeptSettingOnly`: type «тул» → no results, «звук» → «Звук» row.
- [ ] **Step 2:** FAIL. **Step 3:** implement (search field filters with `filter(term, IOS_SEARCHABLE_KEYS)`, results replace the lists while the query is non-empty). **Step 4:** PASS.
- [ ] **Step 5:** Commit `feat: iOS settings main screen`.

### Task 5: Keyboards

**Files:** Create `settings/ios/screens/IosKeyboardsScreen.kt`, `IosAddKeyboardScreen.kt`, `IosLayoutScreen.kt`; «Словари» row opens existing `DictionaryScreen` (wrap its content in `IosScreen` only if it's a small change; otherwise leave as is).
Test: `IosKeyboardsScreenTest.kt`.

- Rows: language display name + layout display name in grey; drag handle via `sh.calvin.reorderable` calling `moveEnabledSubtype`; swipe-to-delete or an «Изменить» edit mode with red minus — builder's choice, but the last remaining language has no delete control (Review Focus 1).
- «Добавить клавиатуру…» → languages not yet enabled (system locales first, then alphabetical) → `IosLayoutScreen` → enables via `SubtypeSettings.addEnabledSubtype` (appended at the end).
- Language row → `IosLayoutScreen` for that subtype: checkmark list of main layouts; change via the same calls `SubtypeScreen.setCurrentSubtype` uses.
- [ ] **Step 1: Failing tests** — `lastLanguageCannotBeRemoved` (only ru enabled → no delete affordance node), `addKeyboardAppendsAtEnd` (add de via the flow → enabled order ends with de), `rowsShowLayoutName` («ЙЦУКЕН» or the layout's display name next to «Русский»).
- [ ] **Steps 2–4**, **Step 5:** commit `feat: iOS keyboards screen`.

### Task 6: Text replacement, Appearance, About

**Files:** Create `settings/ios/screens/IosTextReplacementScreen.kt`, `IosAppearanceScreen.kt`, `IosAboutScreen.kt`. Test: `IosAppearanceScreenTest.kt`.

- Text replacement: list (shortcut bold, phrase grey, language caption when not "all languages"), «+» in the top bar → sheet with Фраза / Сокращение, swipe/edit to delete.
- Appearance: Тема (checkmark list SYSTEM/LIGHT/DARK via `IosThemeChoice`), Шрифт (checkmark list of `KeyboardFont` entries), Ряд цифр, Размытие фона (`PREF_IOS_BLUR`).
- About: app icon + «Glasskey», version, «Исходный код» → source link, «Лицензии» (GPL-3.0 + `docs/third-party.md` items as a plain list).
- [ ] **Step 1: Failing tests** — `themeRowWritesChoice` (tap «Тёмная» → `IosThemeChoice.read == DARK`), `fontRowWritesPref` (tap «Onest» → `PREF_KEYBOARD_FONT == "onest"`).
- [ ] **Steps 2–4**, **Step 5:** commit `feat: iOS text replacement, appearance and about screens`.

### Task 7: Device check and polish

- [ ] Full suite (same 5 baseline failures only).
- [ ] On the phone (user confirms install): dark and light system mode; scroll main screen (title collapses, glass bar appears); press-and-hold a switch (glass thumb); reorder languages then use the globe; add and remove a language; add a text replacement «омв» → «on my way» and type it; Тема Тёмная/Светлая; Размытие фона off → keyboard panel opaque; search «звук».
- [ ] Screenshot main screen next to the mockup, fix visible mismatches.
- [ ] Commit `fix: settings polish from device check`.
