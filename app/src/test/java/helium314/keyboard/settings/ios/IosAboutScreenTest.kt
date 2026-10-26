// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.ios

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import helium314.keyboard.latin.BuildConfig
import helium314.keyboard.settings.ios.screens.IosAboutScreen
import helium314.keyboard.settings.ios.screens.IosLicensesScreen
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "ru-w402dp-h2000dp")
class IosAboutScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun aboutShowsVersionAndLinks() {
        val opened = mutableListOf<String>()
        val routes = mutableListOf<String>()
        val uriHandler = object : UriHandler { override fun openUri(uri: String) { opened += uri } }
        compose.setContent {
            CompositionLocalProvider(LocalUriHandler provides uriHandler) {
                IosSettingsTheme(dark = true) { IosAboutScreen(onBack = {}, navigate = { routes += it }) }
            }
        }
        compose.onNodeWithText(BuildConfig.VERSION_NAME).assertExists()
        compose.onNodeWithText("Исходный код").performClick()
        compose.onNodeWithText("Лицензии").performClick()
        assertEquals(listOf("https://github.com/r415uk3/glasskey"), opened)
        assertEquals(listOf(IosRoute.Licenses), routes)
    }

    @Test fun licensesListGplAndThirdParty() {
        compose.setContent { IosSettingsTheme(dark = true) { IosLicensesScreen(onBack = {}) } }
        listOf(
            "Glasskey", "HeliBoard", "Inter 4.1", "Golos Text", "Onest", "Noto Sans", "Названия эмодзи (Unicode CLDR)",
            "Казахский словарь (aosp-dictionaries)", "AndroidHiddenApiBypass", "Backdrop (AndroidLiquidGlass) 1.0.1",
        )
            .forEach { compose.onNodeWithText(it).assertExists() }
        assertEquals(2, compose.onAllNodesWithText("GPL-3.0-only").fetchSemanticsNodes().size)
    }
}
