// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.ios

import android.content.Context
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.prefs
import helium314.keyboard.settings.ios.screens.IosAppearanceScreen
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// tall screen so the whole lazy list is composed
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "ru-w402dp-h2000dp")
class IosAppearanceScreenTest {
    @get:Rule val compose = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val prefs get() = context.prefs()

    @Before @After fun reset() = prefs.edit(commit = true) {
        listOf(
            Settings.PREF_THEME_DAY_NIGHT, Settings.PREF_THEME_COLORS, Settings.PREF_THEME_COLORS_NIGHT,
            Settings.PREF_KEYBOARD_FONT, Settings.PREF_SHOW_NUMBER_ROW, Settings.PREF_IOS_BLUR,
        ).forEach { remove(it) }
    }

    private fun show() = compose.setContent { IosSettingsTheme(dark = true) { IosAppearanceScreen(onBack = {}) } }

    @Test fun themeRowWritesChoice() {
        show()
        compose.onNodeWithText("Как в системе").assertIsSelected()
        compose.onNodeWithText("Тёмная").performClick()
        compose.waitForIdle()
        assertEquals(IosThemeChoice.DARK, IosThemeChoice.read(prefs))
        compose.onNodeWithText("Тёмная").assertIsSelected()
        compose.onNodeWithText("Светлая").performClick()
        compose.waitForIdle()
        assertEquals(IosThemeChoice.LIGHT, IosThemeChoice.read(prefs))
    }

    @Test fun fontRowWritesPref() {
        show()
        compose.onNodeWithText("Inter").assertIsSelected()
        compose.onNodeWithText("Onest").performClick()
        compose.waitForIdle()
        assertEquals("onest", prefs.getString(Settings.PREF_KEYBOARD_FONT, null))
        compose.onNodeWithText("Onest").assertIsSelected()
        compose.onNodeWithText("Системный").performClick()
        compose.waitForIdle()
        assertEquals("system", prefs.getString(Settings.PREF_KEYBOARD_FONT, null))
    }

    @Test fun switchesWritePrefs() {
        show()
        compose.onNodeWithText("Фон клавиатуры просвечивает, как стекло.").assertExists()
        compose.onNodeWithText("Размытие фона").performClick()
        compose.onNodeWithText("Ряд цифр").performClick()
        compose.waitForIdle()
        assertFalse(prefs.getBoolean(Settings.PREF_IOS_BLUR, true))
        assertTrue(prefs.getBoolean(Settings.PREF_SHOW_NUMBER_ROW, false))
    }
}
