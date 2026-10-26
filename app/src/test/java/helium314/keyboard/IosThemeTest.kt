// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard

import android.content.Context
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import helium314.keyboard.keyboard.KeyboardTheme
import helium314.keyboard.latin.common.ColorType
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.prefs
import helium314.keyboard.settings.SettingsActivity
import org.junit.After
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class IosThemeTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @After fun tearDown() {
        SettingsActivity.forceNight = null
    }

    @Test fun defaultStyleIsIos() {
        assertEquals(5, KeyboardTheme.getKeyboardTheme(context).themeId)
    }

    @Test fun iosIgnoresBordersPref() {
        context.prefs().edit { putBoolean(Settings.PREF_THEME_KEY_BORDERS, true) }
        assertEquals(5, KeyboardTheme.getKeyboardTheme(context).themeId)
    }

    @Test fun darkColors() {
        SettingsActivity.forceNight = true
        val colors = KeyboardTheme.getColorsForCurrentTheme(context)
        assertEquals(0x45FFFFFF, colors.get(ColorType.KEY_BACKGROUND))
        assertEquals(0x45FFFFFF, colors.get(ColorType.FUNCTIONAL_KEY_BACKGROUND))
        assertEquals(0x45FFFFFF, colors.get(ColorType.SPACE_BAR_BACKGROUND))
        assertEquals(0xFFFFFFFF.toInt(), colors.get(ColorType.KEY_TEXT))
        assertEquals(0xDB1C1C1C.toInt(), colors.get(ColorType.MAIN_BACKGROUND))
        assertEquals(0xFF0088FF.toInt(), colors.get(ColorType.ACTION_KEY_BACKGROUND))
    }

    @Test fun lightColors() {
        SettingsActivity.forceNight = false
        val colors = KeyboardTheme.getColorsForCurrentTheme(context)
        assertEquals(0xE3FFFFFF.toInt(), colors.get(ColorType.KEY_BACKGROUND))
        assertEquals(0xE3FFFFFF.toInt(), colors.get(ColorType.FUNCTIONAL_KEY_BACKGROUND))
        assertEquals(0xA6000000.toInt(), colors.get(ColorType.KEY_TEXT))
        assertEquals(0xBFDADADA.toInt(), colors.get(ColorType.MAIN_BACKGROUND))
        assertEquals(0xFF0088FF.toInt(), colors.get(ColorType.ACTION_KEY_BACKGROUND))
    }

    @Test fun iosColorsHaveVisibleKeys() {
        // stock no-border mode makes unpressed keys transparent; iOS keys must stay visible
        context.prefs().edit { putBoolean(Settings.PREF_THEME_KEY_BORDERS, false) }
        SettingsActivity.forceNight = true
        assertEquals(true, KeyboardTheme.getColorsForCurrentTheme(context).hasKeyBorders)
    }
}
