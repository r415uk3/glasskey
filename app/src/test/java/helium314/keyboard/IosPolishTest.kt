// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard

import android.graphics.Color
import androidx.core.content.edit
import helium314.keyboard.keyboard.KeyboardTheme
import helium314.keyboard.latin.LatinIME
import helium314.keyboard.latin.RichInputMethodSubtype
import helium314.keyboard.latin.common.ColorType
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.LanguageOnSpacebarUtils
import helium314.keyboard.latin.utils.SubtypeSettings
import helium314.keyboard.latin.utils.prefs
import helium314.keyboard.settings.SettingsActivity
import org.junit.After
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/** Details found when comparing the device with the Figma reference. */
@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowInputMethodManager2::class])
class IosPolishTest {
    private val latinIME = Robolectric.setupService(LatinIME::class.java)

    private fun useStyle(style: String) {
        latinIME.prefs().edit { putString(Settings.PREF_THEME_STYLE, style) }
        Settings.getInstance().loadSettings(latinIME)
    }

    @After fun tearDown() {
        SettingsActivity.forceNight = null
        latinIME.prefs().edit { remove(Settings.PREF_THEME_STYLE) }
        Settings.getInstance().loadSettings(latinIME)
    }

    private fun spacebarFormat(): Int {
        val enabled = SubtypeSettings.getEnabledSubtypes()
        LanguageOnSpacebarUtils.setEnabledSubtypes(enabled)
        return LanguageOnSpacebarUtils.getLanguageOnSpacebarFormatType(RichInputMethodSubtype.get(enabled.first()))
    }

    @Test fun iosSpacebarShowsTheLanguageNameWhenSwitching() {
        // shown briefly on a switch, then faded out to 0 by the iOS animator (badge in the corner stays)
        useStyle(KeyboardTheme.STYLE_IOS)
        assertEquals(LanguageOnSpacebarUtils.FORMAT_TYPE_LANGUAGE_ONLY, spacebarFormat())
    }

    @Test fun stockStylesStillShowLanguageOnSpacebar() {
        useStyle(KeyboardTheme.STYLE_MATERIAL)
        assertNotEquals(LanguageOnSpacebarUtils.FORMAT_TYPE_NONE, spacebarFormat())
    }

    @Test fun iosStripIsTransparentSoThePanelCornersShow() {
        useStyle(KeyboardTheme.STYLE_IOS)
        SettingsActivity.forceNight = true
        assertEquals(Color.TRANSPARENT, KeyboardTheme.getColorsForCurrentTheme(latinIME).get(ColorType.STRIP_BACKGROUND))
    }

    @Test fun iosDarkPanelMatchesFigmaOnBlack() {
        SettingsActivity.forceNight = true
        assertEquals(0xFF232323.toInt(), KeyboardTheme.getColorsForCurrentTheme(latinIME).get(ColorType.MAIN_BACKGROUND))
    }
}
