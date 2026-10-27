// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.ios

import android.content.Context
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import helium314.keyboard.settings.ios.screens.IosDeleteTag
import helium314.keyboard.settings.ios.screens.IosSheetTag
import helium314.keyboard.settings.ios.screens.IosTextReplacementScreen
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "ru-w402dp-h2000dp")
class IosTextReplacementScreenTest {
    @get:Rule val compose = createComposeRule()

    private class FakeStore(vararg initial: TextReplacement) : TextReplacementStore {
        val entries = initial.toMutableList()
        override fun all(context: Context) = entries.toList()
        override fun add(context: Context, word: String, shortcut: String, locale: Locale?) {
            entries += TextReplacement(word, shortcut, locale)
        }
        override fun delete(context: Context, r: TextReplacement) { entries -= r }
    }

    private fun show(store: TextReplacementStore) =
        compose.setContent { IosSettingsTheme(dark = true) { IosTextReplacementScreen(onBack = {}, store = store) } }

    @Test fun rowsShowPhraseShortcutAndLanguage() {
        show(FakeStore(TextReplacement("on my way!", "omw", null), TextReplacement("сәлем", "слм", Locale.forLanguageTag("kk"))))
        compose.onNodeWithText("on my way!").assertExists()
        compose.onNodeWithText("omw").assertExists()
        compose.onNodeWithText("слм").assertExists()
        // languages are named like on the keyboards screen; only the per-language entry has a caption
        compose.onNodeWithText("Қазақ тілі").assertExists()
        assertEquals(1, compose.onAllNodes(hasText("тілі", substring = true)).fetchSemanticsNodes().size)
    }

    @Test fun plusAddsEntryForAllLanguages() {
        val store = FakeStore()
        show(store)
        compose.onNodeWithContentDescription("Добавить").performClick()
        compose.onNodeWithText("Готово").performClick() // empty: nothing is added
        assertEquals(0, store.entries.size)
        val fields = compose.onAllNodes(hasSetTextAction())
        fields[0].performTextInput("до свидания")
        fields[1].performTextInput("дсв")
        compose.onNodeWithText("Готово").performClick()
        compose.waitForIdle()
        assertEquals(listOf(TextReplacement("до свидания", "дсв", null)), store.entries)
        compose.onNodeWithText("дсв").assertExists()
    }

    @Test fun sheetScalesInAndOut() {
        show(FakeStore())
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        compose.onNodeWithContentDescription("Добавить").performClick()
        compose.mainClock.advanceTimeBy(80)
        val opening = sheetWidth()
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        val open = sheetWidth()
        assertTrue(opening < open - 1f, "opening $opening, open $open") // scaled up from 0.9
        compose.mainClock.autoAdvance = false
        compose.onNodeWithText("Отменить").performClick()
        compose.waitForIdle() // lets the dialog window pick up the click (the clock stays paused)
        compose.mainClock.advanceTimeBy(150)
        val closing = sheetWidth() // still there, on its way out
        assertTrue(closing < open - 1f, "closing $closing, open $open")
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        compose.onAllNodesWithTag(IosSheetTag).assertCountEquals(0)
    }

    private fun sheetWidth() = compose.onNodeWithTag(IosSheetTag).fetchSemanticsNode().boundsInRoot.width

    @Test fun editModeDeletes() {
        val omw = TextReplacement("on my way!", "omw", null)
        val store = FakeStore(omw, TextReplacement("by the way", "btw", null))
        show(store)
        compose.onNodeWithText("Изменить").performClick()
        compose.onAllNodesWithTag(IosDeleteTag)[0].performClick()
        compose.onNodeWithText("Удалить").performClick()
        compose.waitForIdle()
        assertEquals(listOf("btw"), store.entries.map { it.shortcut })
        compose.onNodeWithText("by the way").assertExists()
    }

    @Test fun tapEditsEntryKeepingItsLanguage() {
        val kk = Locale.forLanguageTag("kk")
        val store = FakeStore(TextReplacement("сәлем", "слм", kk))
        show(store)
        compose.onNodeWithText("сәлем").performClick()
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("!")
        compose.onNodeWithText("Готово").performClick()
        compose.waitForIdle()
        assertEquals(listOf(TextReplacement("сәлем!", "слм", kk)), store.entries)
    }

    @Test fun sheetFocusesPhraseOnOpen() {
        show(FakeStore())
        compose.onNodeWithContentDescription("Добавить").performClick()
        compose.waitForIdle()
        compose.onAllNodes(hasSetTextAction())[0].assertIsFocused()
    }

    @Test fun tappingRowLabelFocusesItsField() {
        show(FakeStore())
        compose.onNodeWithContentDescription("Добавить").performClick()
        compose.onNodeWithText("Сокращение").performClick()
        compose.waitForIdle()
        compose.onAllNodes(hasSetTextAction())[1].assertIsFocused()
        compose.onNodeWithText("Фраза").performClick()
        compose.waitForIdle()
        compose.onAllNodes(hasSetTextAction())[0].assertIsFocused()
    }
}
