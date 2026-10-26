// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard

import androidx.core.content.edit
import helium314.keyboard.keyboard.KeyboardTheme
import helium314.keyboard.latin.LatinIME
import helium314.keyboard.latin.ios.IosBlur
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.settings.SettingsSubtype.Companion.toSettingsSubtype
import helium314.keyboard.latin.utils.LayoutType
import helium314.keyboard.latin.utils.SubtypeSettings
import helium314.keyboard.latin.utils.SubtypeUtilsAdditional
import helium314.keyboard.latin.utils.locale
import helium314.keyboard.latin.utils.prefs
import helium314.keyboard.settings.SettingsContainer
import helium314.keyboard.settings.ios.IOS_SEARCHABLE_KEYS
import helium314.keyboard.settings.ios.IosThemeChoice
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowInputMethodManager2::class])
class IosSettingsDataTest {
    private val latinIME = Robolectric.setupService(LatinIME::class.java)
    private val prefs get() = latinIME.prefs()

    private fun tags() = SubtypeSettings.getEnabledSubtypes().map { it.locale().toLanguageTag() }

    // prefs survive between tests in this setup; start and end at the shipped defaults
    @Before @After fun resetPrefs() {
        prefs.edit {
            remove(Settings.PREF_ENABLED_SUBTYPES); remove(Settings.PREF_SELECTED_SUBTYPE)
            remove(Settings.PREF_THEME_DAY_NIGHT); remove(Settings.PREF_THEME_COLORS); remove(Settings.PREF_IOS_BLUR)
            remove(Settings.PREF_ADDITIONAL_SUBTYPES)
        }
        SubtypeSettings.reloadEnabledSubtypes(latinIME)
    }

    @Test fun enabledOrderIsKeptAndMovable() {
        assertEquals(listOf("ru", "en-US", "kk"), tags())
        SubtypeSettings.moveEnabledSubtype(latinIME, 2, 0) // kk to top
        assertEquals(listOf("kk", "ru", "en-US"), tags())
        SubtypeSettings.reloadEnabledSubtypes(latinIME) // survives reload from prefs
        assertEquals(listOf("kk", "ru", "en-US"), tags())
        SubtypeSettings.moveEnabledSubtype(latinIME, 0, 2) // and back down
        assertEquals(listOf("ru", "en-US", "kk"), tags())
    }

    @Test fun addedLanguagesAreAppendedNotSorted() {
        val (ru, en, kk) = SubtypeSettings.getEnabledSubtypes()
        prefs.edit { putString(Settings.PREF_ENABLED_SUBTYPES, "") }
        SubtypeSettings.reloadEnabledSubtypes(latinIME)
        listOf(kk, ru, en).forEach { SubtypeSettings.addEnabledSubtype(prefs, it) }
        assertEquals(listOf("kk", "ru", "en-US"), tags())
        SubtypeSettings.reloadEnabledSubtypes(latinIME)
        assertEquals(listOf("kk", "ru", "en-US"), tags())
    }

    @Test fun editedLanguageKeepsItsPlace() {
        val (ru, en, kk) = SubtypeSettings.getEnabledSubtypes()
        prefs.edit { putString(Settings.PREF_ENABLED_SUBTYPES, "") }
        SubtypeSettings.reloadEnabledSubtypes(latinIME)
        listOf(ru, en, kk).forEach { SubtypeSettings.addEnabledSubtype(prefs, it) }
        val to = en.toSettingsSubtype().withLayout(LayoutType.SYMBOLS, "symbols_arabic")
        SubtypeUtilsAdditional.changeAdditionalSubtype(en.toSettingsSubtype(), to, latinIME)
        assertEquals(listOf("ru", "en-US", "kk"), tags())
        assertEquals(to, SubtypeSettings.getEnabledSubtypes()[1].toSettingsSubtype())
    }

    @Test fun defaultsAreInResourceForm() {
        val stored = SubtypeSettings.createSettingsSubtypes(Defaults.PREF_ENABLED_SUBTYPES)
        assertEquals(SubtypeSettings.getEnabledSubtypes().map { it.toSettingsSubtype() }, stored)
        val ru = SubtypeSettings.getEnabledSubtypes()[0]
        assertEquals(ru, SubtypeSettings.getSelectedSubtype(prefs))
        SubtypeSettings.moveEnabledSubtype(latinIME, 1, 0) // ru no longer first: still selected
        assertEquals(ru, SubtypeSettings.getSelectedSubtype(prefs))
    }

    @Test fun editingRuOnUntouchedDefaultsKeepsItEnabledAndSelected() {
        val ru = SubtypeSettings.getEnabledSubtypes()[0]
        val to = ru.toSettingsSubtype().withLayout(LayoutType.SYMBOLS, "symbols_arabic")
        SubtypeUtilsAdditional.changeAdditionalSubtype(ru.toSettingsSubtype(), to, latinIME)
        assertEquals(listOf("ru", "en-US", "kk"), tags())
        assertEquals(to, SubtypeSettings.getEnabledSubtypes()[0].toSettingsSubtype())
        assertEquals(to, SubtypeSettings.getSelectedSubtype(prefs).toSettingsSubtype())
    }

    @Test fun themeChoiceRoundTrip() {
        assertEquals(IosThemeChoice.SYSTEM, IosThemeChoice.read(prefs)) // defaults, nothing stored
        for (c in IosThemeChoice.entries) {
            IosThemeChoice.write(prefs, c)
            assertEquals(c, IosThemeChoice.read(prefs))
        }
    }

    @Test fun darkChoiceUsesIosDarkColors() {
        IosThemeChoice.write(prefs, IosThemeChoice.DARK)
        assertEquals(false, prefs.getBoolean(Settings.PREF_THEME_DAY_NIGHT, true))
        assertEquals(KeyboardTheme.THEME_IOS_DARK, prefs.getString(Settings.PREF_THEME_COLORS, null))
        IosThemeChoice.write(prefs, IosThemeChoice.LIGHT)
        assertEquals(false, prefs.getBoolean(Settings.PREF_THEME_DAY_NIGHT, true))
        assertEquals(KeyboardTheme.THEME_IOS_LIGHT, prefs.getString(Settings.PREF_THEME_COLORS, null))
        IosThemeChoice.write(prefs, IosThemeChoice.SYSTEM)
        assertEquals(true, prefs.getBoolean(Settings.PREF_THEME_DAY_NIGHT, false))
    }

    // Robolectric has no cross-window blur, so this only shows the switch can turn it off
    @Test fun blurOffMeansNoPanelBlur() {
        prefs.edit { putBoolean(Settings.PREF_IOS_BLUR, false) }
        assertEquals(false, IosBlur.isAvailable(latinIME))
    }

    @Test fun searchFindsOnlyKeptSettings() {
        val c = SettingsContainer(latinIME)
        IOS_SEARCHABLE_KEYS.forEach { assertTrue(c[it] != null, "$it is not a container setting") }
        val auto = c.filter("auto", IOS_SEARCHABLE_KEYS)
        assertTrue(auto.isNotEmpty())
        assertTrue(auto.all { it.key in IOS_SEARCHABLE_KEYS })
        assertTrue(c.filter("toolbar").isNotEmpty()) // the unrestricted search still finds them
        assertTrue(c.filter("toolbar", IOS_SEARCHABLE_KEYS).isEmpty())
    }
}
