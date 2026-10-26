// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard

import helium314.keyboard.latin.LatinIME
import helium314.keyboard.latin.dictionary.DictionaryInfoUtils
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.SubtypeSettings
import helium314.keyboard.latin.utils.ToolbarMode
import helium314.keyboard.latin.utils.locale
import helium314.keyboard.latin.utils.mainLayoutNameOrQwerty
import helium314.keyboard.latin.utils.prefs
import androidx.core.content.edit
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
class IosDefaultsTest {
    private val latinIME = Robolectric.setupService(LatinIME::class.java)

    // prefs survive between tests in this setup; start from the shipped defaults
    @Before fun freshLanguagePrefs() {
        latinIME.prefs().edit { remove(Settings.PREF_ENABLED_SUBTYPES); remove(Settings.PREF_SELECTED_SUBTYPE) }
        SubtypeSettings.reloadEnabledSubtypes(latinIME)
    }

    @Test fun enabledSubtypesAreRuEnKk() {
        val subtypes = SubtypeSettings.getEnabledSubtypes()
        assertEquals(listOf("ru", "en-US", "kk"), subtypes.map { it.locale().toLanguageTag() })
        assertEquals(listOf("russian", "qwerty", "russian"), subtypes.map { it.mainLayoutNameOrQwerty() })
    }

    @Test fun selectedIsRussian() {
        assertEquals("ru", SubtypeSettings.getSelectedSubtype(latinIME.prefs()).locale().toLanguageTag())
    }

    @Test fun toolbarModeIsSuggestionStrip() {
        assertEquals(ToolbarMode.SUGGESTION_STRIP, Settings.getValues().mToolbarMode)
    }

    @Test fun iosBehaviourDefaults() {
        assertEquals(false, Settings.getValues().mSpaceForLangChange)
        assertEquals(false, Defaults.PREF_SHOW_HINTS)
        assertEquals(true, Defaults.PREF_VIBRATE_ON)
        assertEquals(true, Defaults.PREF_SOUND_ON)
        assertEquals(false, Defaults.PREF_ENABLE_CLIPBOARD_HISTORY)
        assertEquals(false, Defaults.PREF_SHOW_SETUP_WIZARD_ICON)
        assertEquals(true, Settings.getValues().mKeyPreviewPopupOn)
    }

    @Test fun kkDictionaryIsBundled() {
        assertTrue("main_kk.dict" in DictionaryInfoUtils.getAssetsDictionaryList(latinIME).orEmpty())
    }
}
