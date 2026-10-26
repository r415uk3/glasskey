// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard

import android.view.inputmethod.EditorInfo
import androidx.core.content.edit
import helium314.keyboard.keyboard.KeyboardElement
import helium314.keyboard.keyboard.KeyboardTheme
import helium314.keyboard.latin.LatinIME
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.LayoutType
import helium314.keyboard.latin.utils.SubtypeUtilsAdditional
import helium314.keyboard.latin.utils.prefs
import org.junit.After
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Glasskey changes defaults for the iOS style only; the stock styles keep HeliBoard's behaviour. */
@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowInputMethodManager2::class, ShadowProximityInfo::class])
class StyleDefaultsTest {
    private val latinIME = Robolectric.setupService(LatinIME::class.java)
    private val prefs = latinIME.prefs()

    private fun useStyle(style: String) {
        prefs.edit { putString(Settings.PREF_THEME_STYLE, style) }
        Settings.getInstance().loadSettings(latinIME)
    }

    @After fun tearDown() {
        prefs.edit { remove(Settings.PREF_THEME_STYLE) }
        Settings.getInstance().loadSettings(latinIME)
    }

    @Test fun stockStylesKeepStockLayouts() {
        useStyle(KeyboardTheme.STYLE_MATERIAL)
        assertEquals("functional_keys", Settings.readDefaultLayoutName(LayoutType.FUNCTIONAL, prefs))
        assertEquals("symbols", Settings.readDefaultLayoutName(LayoutType.SYMBOLS, prefs))
        assertEquals("symbols_shifted", Settings.readDefaultLayoutName(LayoutType.MORE_SYMBOLS, prefs))
    }

    @Test fun iosStyleUsesIosLayouts() {
        useStyle(KeyboardTheme.STYLE_IOS)
        assertEquals("ios_functional", Settings.readDefaultLayoutName(LayoutType.FUNCTIONAL, prefs))
        assertEquals("ios_symbols", Settings.readDefaultLayoutName(LayoutType.SYMBOLS, prefs))
        assertEquals("ios_symbols_shifted", Settings.readDefaultLayoutName(LayoutType.MORE_SYMBOLS, prefs))
    }

    @Test fun stockStylesSwitchLanguageWithSpaceLongPress() {
        useStyle(KeyboardTheme.STYLE_MATERIAL)
        assertEquals(true, Settings.getValues().mSpaceForLangChange)
        useStyle(KeyboardTheme.STYLE_IOS)
        assertEquals(false, Settings.getValues().mSpaceForLangChange)
    }

    @Test fun twoKeyLastRowLayoutsKeepTheirLetters() {
        useStyle(KeyboardTheme.STYLE_IOS)
        val subtype = SubtypeUtilsAdditional.createEmojiCapableAdditionalSubtype(Locale.ENGLISH, "dvorak", true)
        val labels = buildTestKeyboard(latinIME, EditorInfo(), subtype, KeyboardElement.ALPHABET).first.sortedKeys.mapNotNull { it.label }
        assertTrue("q" in labels && "z" in labels, labels.toString())
    }
}
