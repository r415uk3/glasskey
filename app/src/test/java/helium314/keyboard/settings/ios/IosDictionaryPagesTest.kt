// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.ios

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.test.core.app.ApplicationProvider
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import helium314.keyboard.settings.ios.screens.dictionaryLanguageName
import androidx.compose.ui.test.performClick
import helium314.keyboard.settings.ios.screens.IosDictionaryCandidate
import helium314.keyboard.settings.ios.screens.IosDictionaryFileScreen
import helium314.keyboard.settings.ios.screens.IosDictionaryInfo
import helium314.keyboard.settings.ios.screens.IosDictionaryLanguageScreen
import helium314.keyboard.settings.ios.screens.IosDictionaryStore
import helium314.keyboard.settings.ios.screens.IosNewDictionarySheet
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "ru-w402dp-h2000dp")
class IosDictionaryPagesTest {
    @get:Rule val compose = createComposeRule()

    private val kk = Locale.forLanguageTag("kk")
    private fun title(l: Locale) = dictionaryLanguageName(l, ApplicationProvider.getApplicationContext())
    private val builtIn = IosDictionaryInfo("main", builtIn = true, version = null, date = null, description = null)
    private val emoji = IosDictionaryInfo("emoji", builtIn = false, version = "2", date = "12.03.2025", description = "Emoji for kk")

    private class FakeStore(vararg initial: IosDictionaryInfo) : IosDictionaryStore {
        val entries = initial.toMutableList()
        override fun dictionaries(context: Context, locale: Locale) = entries.toList()
        override fun delete(context: Context, locale: Locale, type: String) { entries.removeAll { it.type == type && !it.builtIn } }
    }

    private fun showLanguage(store: IosDictionaryStore, navigated: MutableList<String> = mutableListOf()) = compose.setContent {
        IosSettingsTheme(dark = true) { IosDictionaryLanguageScreen(kk, onBack = {}, navigate = { navigated += it }, store = store) }
    }

    @Test fun languagePageListsBuiltInAndAddedDictionaries() {
        showLanguage(FakeStore(builtIn, emoji))
        compose.onNodeWithTag(IosLargeTitleTag).assertTextEquals(title(kk))
        assertEquals("К", title(kk).take(1))
        compose.onNodeWithText("Основной словарь").assertExists()
        compose.onNodeWithText("Встроенный").assertExists()
        compose.onNodeWithText("emoji").assertExists()
        compose.onNodeWithText("Добавлен").assertExists()
        compose.onNodeWithText("v 2").assertExists()
        compose.onNodeWithText("Добавить словарь…").assertExists()
    }

    @Test fun addedDictionaryOpensItsPage() {
        val navigated = mutableListOf<String>()
        showLanguage(FakeStore(builtIn, emoji), navigated)
        compose.onNodeWithText("emoji").performClick()
        assertEquals(listOf(IosRoute.DictionaryFile + "kk/emoji"), navigated)
    }

    @Test fun replacedBuiltInIsExplained() {
        showLanguage(FakeStore(builtIn.copy(replaced = true), IosDictionaryInfo("main", false, "54", null, null)))
        compose.onNodeWithText("Добавленный основной словарь заменяет встроенный.").assertExists()
    }

    @Test fun dictionaryPageShowsDetailsAndDeletesAfterConfirmation() {
        val store = FakeStore(builtIn, emoji)
        var back = 0
        compose.setContent { IosSettingsTheme(dark = true) { IosDictionaryFileScreen(kk, "emoji", onBack = { back++ }, store = store) } }
        compose.onNodeWithText("2").assertExists()
        compose.onNodeWithText("12.03.2025").assertExists()
        compose.onNodeWithText("Emoji for kk").assertExists()
        compose.onNodeWithText("Удалить словарь").performClick()
        compose.onNodeWithText("Словарь «emoji» будет удалён с телефона.").assertExists()
        assertEquals(2, store.entries.size) // nothing deleted before the confirmation
        compose.onNodeWithText("Отменить").performClick()
        compose.waitForIdle()
        assertEquals(2, store.entries.size)
        compose.onNodeWithText("Удалить словарь").performClick()
        compose.onAllNodesWithText("Удалить словарь").onLast().performClick() // the action sheet's button
        compose.waitForIdle()
        assertEquals(listOf(builtIn), store.entries)
        assertEquals(1, back)
    }

    private val candidate = IosDictionaryCandidate(
        type = "main", version = "54", dictLocale = kk,
        locales = listOf(kk, Locale.forLanguageTag("ru")), initial = kk,
    )

    @Test fun newDictionaryCardAddsForTheChosenLanguage() {
        val added = mutableListOf<Locale>()
        compose.setContent {
            IosSettingsTheme(dark = true) {
                IosNewDictionarySheet(candidate, existing = { null }, onAdd = { added += it }, onDismiss = {})
            }
        }
        compose.onNodeWithText("Новый словарь").assertExists()
        compose.onNodeWithText("54").assertExists()
        compose.onNodeWithText(title(kk)).performClick() // the language row opens the list
        compose.onNodeWithText(title(Locale.forLanguageTag("ru"))).performClick()
        compose.onNodeWithText("Добавить").performClick()
        compose.waitForIdle()
        assertEquals(listOf(Locale.forLanguageTag("ru")), added)
    }

    @Test fun newDictionaryCardWarnsWhenItReplacesOne() {
        compose.setContent {
            IosSettingsTheme(dark = true) {
                IosNewDictionarySheet(candidate, existing = { IosDictionaryInfo("main", false, "18", null, null) }, onAdd = {}, onDismiss = {})
            }
        }
        compose.onNodeWithText("Для этого языка уже есть словарь «main» (v 18). Новый заменит его.").assertExists()
        compose.onNodeWithText("Заменить").assertExists()
    }
}
