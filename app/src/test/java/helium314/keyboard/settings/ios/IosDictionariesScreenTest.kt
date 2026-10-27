// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.ios

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import helium314.keyboard.settings.ios.screens.IosDictionariesScreen
import helium314.keyboard.settings.ios.screens.IosDictionaryLanguage
import helium314.keyboard.settings.ios.screens.iosDictionaryLanguages
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "ru-w402dp-h2000dp")
class IosDictionariesScreenTest {
    @get:Rule val compose = createComposeRule()

    private val fake = listOf(
        IosDictionaryLanguage(Locale.forLanguageTag("kk"), "Қазақ тілі", "Встроенный словарь"),
        IosDictionaryLanguage(Locale.forLanguageTag("ru"), "Русский", "Встроенный словарь, emoji"),
    )

    private fun show(languages: List<IosDictionaryLanguage> = fake, opened: MutableList<Locale> = mutableListOf()) =
        compose.setContent {
            IosSettingsTheme(dark = true) {
                IosDictionariesScreen(onBack = {}, loadLanguages = { languages }, onOpenLanguage = { opened += it })
            }
        }

    @Test fun iosChromeAndRows() {
        show()
        compose.onNodeWithTag(IosLargeTitleTag).assertExists()
        compose.onNodeWithTag(IosBackButtonTag).assertExists()
        compose.onNodeWithText("Добавить словарь…").assertExists()
        compose.onNodeWithText("Қазақ тілі").assertExists()
        compose.onNodeWithText("Встроенный словарь, emoji").assertExists()
    }

    @Test fun tapOpensLanguageDetails() {
        val opened = mutableListOf<Locale>()
        show(opened = opened)
        compose.onNodeWithText("Русский").performClick()
        assertTrue(opened.single().language == "ru")
    }

    @Test fun searchFiltersLanguages() {
        show()
        compose.onNode(hasSetTextAction()).performTextInput("рус")
        compose.waitForIdle()
        compose.onAllNodesWithText("Қазақ тілі").assertCountEquals(0)
        compose.onNodeWithText("Русский").assertExists()
    }

    @Test fun realLanguagesIncludeShippedKazakh() {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        assertTrue(iosDictionaryLanguages(ctx).any { it.locale.language == "kk" })
    }
}
