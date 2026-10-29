// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.ios

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performScrollToNode
import android.content.Context
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import helium314.keyboard.latin.R
import helium314.keyboard.latin.utils.runInLocale
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.SubtypeSettings
import helium314.keyboard.latin.utils.locale
import helium314.keyboard.latin.utils.mainLayoutName
import helium314.keyboard.latin.utils.prefs
import helium314.keyboard.settings.ios.screens.IosDeleteTag
import helium314.keyboard.settings.ios.screens.IosDragHandleTag
import helium314.keyboard.settings.ios.screens.IosKeyboardsScreen
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import java.util.Locale

// tall screen so the whole lazy list is composed
@RunWith(RobolectricTestRunner::class)
// no ShadowInputMethodManager2: RichInputMethodManager keeps the first test's InputMethodManager, whose shadow
// may be the stock one, and its background refresh would fail to cast it
@Config(qualifiers = "ru-w402dp-h2000dp")
class IosKeyboardsScreenTest {
    @get:Rule val compose = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val prefs get() = context.prefs()

    private fun tags() = SubtypeSettings.getEnabledSubtypes().map { it.locale().toLanguageTag() }

    // prefs survive between tests; start and end at the shipped defaults (ru, en-US, kk)
    @Before @After fun reset() {
        prefs.edit {
            remove(Settings.PREF_ENABLED_SUBTYPES); remove(Settings.PREF_SELECTED_SUBTYPE)
            remove(Settings.PREF_ADDITIONAL_SUBTYPES)
        }
        SubtypeSettings.reloadEnabledSubtypes(context)
    }

    private fun showKeyboards(navigate: (String) -> Unit = {}) =
        compose.setContent { IosSettingsTheme(dark = true) { IosKeyboardsScreen(onBack = {}, navigate = navigate) } }

    private fun showApp() =
        compose.setContent { IosSettingsTheme(dark = true) { IosSettingsNavHost(onClickBack = {}, needsEnabling = false, onEnable = {}) } }

    @Test fun rowsShowLayoutName() {
        showKeyboards()
        compose.onNodeWithText("Русский").assertExists()
        assertEquals(2, compose.onAllNodesWithText("ЙЦУКЕН").fetchSemanticsNodes().size) // ru and kk
        compose.onNodeWithText("QWERTY").assertExists()
        compose.onNodeWithText("Нажатие на глобус возвращает прошлый язык", substring = true).assertExists()
    }

    // English (US) is named via runInLocale, which must not switch the app's own strings to English
    @Test fun runInLocaleKeepsAppLocale() {
        assertEquals("Keyboards", runInLocale(context.resources, Locale.US) { it.getString(R.string.ios_keyboards) })
        assertEquals("Клавиатуры", context.getString(R.string.ios_keyboards))
    }

    @Test fun lastLanguageCannotBeRemoved() {
        prefs.edit { putString(Settings.PREF_ENABLED_SUBTYPES, "") }
        SubtypeSettings.reloadEnabledSubtypes(context)
        SubtypeSettings.addEnabledSubtype(prefs, SubtypeSettings.getResourceSubtypesForLocale(Locale("ru")).first())
        assertEquals(listOf("ru"), tags())
        showKeyboards()
        compose.onNodeWithText("Изменить").performClick()
        compose.onAllNodesWithTag(IosDeleteTag).assertCountEquals(0)
    }

    @Test fun editModeRemovesLanguage() {
        showKeyboards()
        compose.onNodeWithText("Изменить").performClick()
        compose.onAllNodesWithTag(IosDeleteTag).assertCountEquals(3)
        compose.onAllNodesWithTag(IosDeleteTag)[2].performClick() // kk
        compose.onNodeWithText("Удалить").performClick()
        compose.waitForIdle()
        assertEquals(listOf("ru", "en-US"), tags())
        compose.onAllNodesWithTag(IosDeleteTag).assertCountEquals(2)
    }

    @Test fun dragHandleReorders() {
        showKeyboards()
        compose.onNodeWithText("Изменить").performClick()
        compose.onAllNodesWithTag(IosDragHandleTag)[2].performTouchInput { // kk to the top
            down(center)
            repeat(20) { moveBy(Offset(0f, -15f)) }
            up()
        }
        compose.waitForIdle()
        assertEquals(listOf("kk", "ru", "en-US"), tags())
    }

    @Test fun dictionariesRowNavigates() {
        val routes = mutableListOf<String>()
        showKeyboards(navigate = { routes += it })
        compose.onNodeWithText("Подсказки для языков без встроенного словаря.").assertExists()
        compose.onNodeWithText("Словари").performClick()
        compose.onNodeWithText("Добавить клавиатуру…").performClick()
        assertEquals(listOf(IosRoute.Dictionaries, IosRoute.AddKeyboard), routes)
    }

    @Test fun addKeyboardAppendsAtEnd() {
        showApp()
        compose.onNodeWithText("Клавиатуры").performClick()
        compose.onNodeWithText("Добавить клавиатуру…").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Казахский").assertDoesNotExist() // already enabled
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Немецкий"))
        compose.onNodeWithText("Немецкий").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("QWERTZ").performClick()
        compose.waitForIdle()
        assertEquals(listOf("ru", "en-US", "kk", "de"), tags())
        compose.onNodeWithText("Deutsch").assertExists() // back on the keyboards list, named in its own language
        compose.onNodeWithText("Добавить клавиатуру…").assertExists()
    }

    @Test fun languageRowChangesLayoutInPlace() {
        showApp()
        compose.onNodeWithText("Клавиатуры").performClick()
        compose.onNodeWithText("Русский").performClick()
        compose.waitForIdle()
        // by position: layout names come from HeliBoard's static resources, whose locale may differ between test classes
        val layoutRows = compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected))
        layoutRows[0].assertIsSelected() // russian
        layoutRows[1].performClick() // russian_extended
        compose.waitForIdle()
        assertEquals(listOf("ru", "en-US", "kk"), tags())
        assertEquals("russian_extended", SubtypeSettings.getEnabledSubtypes()[0].mainLayoutName())
    }
}
