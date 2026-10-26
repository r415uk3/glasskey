// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.ios

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.prefs
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class IosComponentsTest {
    @get:Rule val compose = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Before @After fun resetPrefs() = context.prefs().edit().remove(Settings.PREF_SOUND_ON).commit().let { }

    @Test fun switchRowWritesPref() {
        compose.setContent { IosSettingsTheme(dark = true) { IosSwitchRow("Звук", Settings.PREF_SOUND_ON, false) } }
        compose.onNodeWithText("Звук").performClick()
        compose.waitForIdle()
        assertTrue(context.prefs().getBoolean(Settings.PREF_SOUND_ON, false))
        compose.onNodeWithText("Звук").performClick()
        compose.waitForIdle()
        assertFalse(context.prefs().getBoolean(Settings.PREF_SOUND_ON, true))
    }

    @Test fun rowWithClickShowsChevronAndCallsBack() {
        var clicks = 0
        compose.setContent { IosSettingsTheme(dark = false) { IosRow("Клавиатуры", value = "3", onClick = { clicks++ }) } }
        compose.onNodeWithText("3").assertIsDisplayed()
        compose.onNodeWithTag(IosRowChevronTag, useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("Клавиатуры").performClick()
        compose.waitForIdle()
        assertEquals(1, clicks)
    }

    @Test fun screenShowsLargeTitleAndBackOnlyWhenGiven() {
        var backs = 0
        compose.setContent {
            IosSettingsTheme(dark = true) {
                IosScreen("Основные", onBack = { backs++ }) {
                    iosGroup(header = "Заголовок", footer = "Сноска", rows = listOf({ IosRow("Строка") }))
                }
            }
        }
        compose.onNodeWithTag(IosLargeTitleTag).assertIsDisplayed()
        compose.onNodeWithText("Заголовок").assertIsDisplayed()
        compose.onNodeWithText("Сноска").assertIsDisplayed()
        compose.onNodeWithText("Строка").assertIsDisplayed()
        compose.onNodeWithTag(IosBackButtonTag).performClick()
        compose.waitForIdle()
        assertEquals(1, backs)
    }

    @Test fun rootScreenHasNoBackButton() {
        compose.setContent { IosSettingsTheme(dark = true) { IosScreen("Настройки", onBack = null) { } } }
        compose.onNodeWithTag(IosLargeTitleTag).assertIsDisplayed()
        compose.onNodeWithTag(IosBackButtonTag).assertDoesNotExist()
    }

    @Test fun checkRowCallsBack() {
        var clicks = 0
        compose.setContent { IosSettingsTheme(dark = true) { IosCheckRow("Русский", checked = true, onClick = { clicks++ }) } }
        compose.onNodeWithText("Русский").performClick()
        compose.waitForIdle()
        assertEquals(1, clicks)
    }

    @Test fun searchFieldReportsQuery() {
        var q = ""
        compose.setContent { IosSettingsTheme(dark = true) { IosSearchField(query = q, onQueryChange = { q = it }) } }
        compose.onNodeWithTag(IosSearchFieldTag).assertIsDisplayed()
        compose.onNode(hasSetTextAction()).performTextInput("звук")
        compose.waitForIdle()
        assertEquals("звук", q)
    }

    @Test fun tapOnSwitchTogglesOnce() {
        compose.setContent { IosSettingsTheme(dark = true) { IosSwitchRow("Звук", Settings.PREF_SOUND_ON, false) } }
        val switch = compose.onNodeWithTag(IosSwitchTag, useUnmergedTree = true)
        switch.performTouchInput { click(center) }
        compose.waitForIdle()
        assertTrue(context.prefs().getBoolean(Settings.PREF_SOUND_ON, false))
        switch.performTouchInput { click(center) }
        compose.waitForIdle()
        assertFalse(context.prefs().getBoolean(Settings.PREF_SOUND_ON, true))
    }

    @Test fun horizontalSwipeSettlesBySide() {
        val changes = mutableListOf<Boolean>()
        compose.setContent {
            IosSettingsTheme(dark = true) {
                var checked by remember { mutableStateOf(false) }
                IosSwitch(checked, { checked = it; changes += it })
            }
        }
        val switch = compose.onNodeWithTag(IosSwitchTag)
        switch.performTouchInput { swipeRight(startX = centerX - 40f, endX = centerX + 200f) }
        compose.waitForIdle()
        assertEquals(listOf(true), changes)
        // dragging further towards the end it already is at changes nothing
        switch.performTouchInput { swipeRight(startX = centerX - 40f, endX = centerX + 200f) }
        compose.waitForIdle()
        assertEquals(listOf(true), changes)
        switch.performTouchInput { swipeLeft(startX = centerX + 40f, endX = centerX - 200f) }
        compose.waitForIdle()
        assertEquals(listOf(true, false), changes)
    }

    @Test fun verticalSwipeOnSwitchScrollsListInsteadOfToggling() {
        lateinit var state: LazyListState
        compose.setContent {
            IosSettingsTheme(dark = true) {
                state = rememberLazyListState()
                LazyColumn(Modifier.height(300.dp), state = state) {
                    item { IosSwitchRow("Звук", Settings.PREF_SOUND_ON, false) }
                    items(40) { IosRow("Строка $it") }
                }
            }
        }
        compose.onNodeWithTag(IosSwitchTag, useUnmergedTree = true)
            .performTouchInput { swipeUp(startY = centerY, endY = centerY - 500f) }
        compose.waitForIdle()
        assertFalse(context.prefs().getBoolean(Settings.PREF_SOUND_ON, false))
        assertTrue(state.firstVisibleItemIndex > 0 || state.firstVisibleItemScrollOffset > 0)
    }
}
