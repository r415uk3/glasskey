// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.ios

import android.content.Context
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.prefs
import helium314.keyboard.settings.SettingsActivity
import helium314.keyboard.settings.SettingsContainer
import helium314.keyboard.settings.ios.screens.IosMainScreen
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

// tall screen so the whole lazy list is composed
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "ru-w402dp-h2000dp")
class IosMainScreenTest {
    @get:Rule val compose = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Before @After fun reset() {
        context.prefs().edit().remove(Settings.PREF_SOUND_ON).commit()
        SettingsActivity.settingsContainer = SettingsContainer(context)
    }

    private fun show(needsEnabling: Boolean = false, onEnable: () -> Unit = {}, navigate: (String) -> Unit = {}) =
        compose.setContent { IosSettingsTheme(dark = true) { IosMainScreen(needsEnabling, onEnable, navigate) } }

    @Test fun mainScreenShowsOnlyKeptItems() {
        show()
        listOf(
            "Клавиатуры", "Замена текста", "Оформление", "ВСЕ КЛАВИАТУРЫ", "Автопрописные", "Автокоррекция",
            "Предиктивный набор", "Просмотр символов", "Быстрая клавиша «.»", "ОТКЛИК", "Звук", "Вибрация", "О Glasskey",
            "Двойное нажатие пробела вставляет точку и пробел.",
        ).forEach { compose.onNodeWithText(it).assertExists() }
        listOf("Toolbar", "Панель инструментов", "Advanced", "Дополнительно", "Дополнительные")
            .forEach { compose.onNodeWithText(it, substring = true).assertDoesNotExist() }
    }

    @Test fun rowsNavigate() {
        val routes = mutableListOf<String>()
        show(navigate = { routes += it })
        compose.onNodeWithText("Клавиатуры").performClick()
        compose.onNodeWithText("Замена текста").performClick()
        compose.onNodeWithText("Оформление").performClick()
        compose.onNodeWithText("О Glasskey").performClick()
        assertEquals(listOf(IosRoute.Keyboards, IosRoute.TextReplacement, IosRoute.Appearance, IosRoute.About), routes)
    }

    @Test fun enableCardOnlyWhenNeeded() {
        var enables = 0
        show(needsEnabling = true, onEnable = { enables++ })
        compose.onNodeWithText("Включить Glasskey").performClick()
        assertEquals(1, enables)
    }

    @Test fun noEnableCardWhenEnabled() {
        show(needsEnabling = false)
        compose.onNodeWithText("Включить Glasskey").assertDoesNotExist()
    }

    @Test fun searchShowsKeptSettingOnly() {
        show()
        val field = compose.onNode(hasSetTextAction())
        field.performTextInput("тул")
        compose.onNodeWithText("Нет результатов").assertExists()
        compose.onNodeWithText("Клавиатуры").assertDoesNotExist()
        field.performTextClearance()
        field.performTextInput("звук")
        compose.onNodeWithText("Звук").assertExists()
        compose.onNodeWithText("Вибрация").assertDoesNotExist()
        compose.onNodeWithText("Нет результатов").assertDoesNotExist()
        compose.onNodeWithText("Звук").performClick() // result is a working switch
        compose.waitForIdle()
        assertFalse(context.prefs().getBoolean(Settings.PREF_SOUND_ON, true))
    }

    @Test fun searchMatchesIosTitlesAndLinksToOwningScreen() {
        val routes = mutableListOf<String>()
        show(navigate = { routes += it })
        compose.onNode(hasSetTextAction()).performTextInput("автопроп")
        compose.onNodeWithText("Автопрописные").assertExists()
        compose.onNode(hasSetTextAction()).performTextClearance()
        compose.onNode(hasSetTextAction()).performTextInput("шрифт")
        compose.onNodeWithText("Шрифт").performClick()
        assertEquals(listOf(IosRoute.Appearance), routes)
    }
}
