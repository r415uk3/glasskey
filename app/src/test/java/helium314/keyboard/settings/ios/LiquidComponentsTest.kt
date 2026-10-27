// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.ios

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import helium314.keyboard.settings.ios.liquid.LiquidButton
import helium314.keyboard.settings.ios.liquid.LiquidToggle
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class LiquidComponentsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun toggleTapFlipsState() {
        compose.setContent {
            IosSettingsTheme(dark = false) {
                var selected by remember { mutableStateOf(false) }
                LiquidToggle(
                    selected = { selected },
                    onSelect = { selected = it },
                    backdrop = rememberIosBackdrop(),
                    modifier = Modifier.testTag("toggle"),
                )
                BasicText(if (selected) "on" else "off")
            }
        }
        compose.onNodeWithTag("toggle").performTouchInput { click(center) }
        compose.waitForIdle()
        compose.onNodeWithText("on").assertExists()
        compose.onNodeWithTag("toggle").performTouchInput { click(center) }
        compose.waitForIdle()
        compose.onNodeWithText("off").assertExists()
    }

    @Test fun shortHorizontalDragSnapsBack() {
        val changes = mutableListOf<Boolean>()
        compose.setContent {
            IosSettingsTheme(dark = true) {
                var checked by remember { mutableStateOf(false) }
                IosSwitch(checked, { checked = it; changes += it })
            }
        }
        val switch = compose.onNodeWithTag(IosSwitchTag)
        // past the touch slop (so it is a drag, not a tap), then back to less than half of the 20dp thumb travel
        // (Robolectric's slop is more than half the travel)
        switch.performTouchInput {
            val start = Offset(centerX - 10.dp.toPx(), centerY)
            down(start)
            moveTo(start + Offset(viewConfiguration.touchSlop / 2, 0f))
            moveTo(start + Offset(viewConfiguration.touchSlop + 1.dp.toPx(), 0f))
            moveTo(start + Offset(4.dp.toPx(), 0f))
            up()
        }
        compose.waitForIdle()
        assertEquals(emptyList(), changes)
        switch.assertIsOff()
    }

    @Test fun switchKeepsSwitchSemantics() {
        compose.setContent {
            IosSettingsTheme(dark = true) {
                var checked by remember { mutableStateOf(true) }
                IosSwitch(checked, { checked = it })
            }
        }
        compose.onNodeWithTag(IosSwitchTag).assertIsOn().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(IosSwitchTag).assertIsOff()
    }

    @Test fun buttonClickFiresOnClick() {
        var clicks = 0
        compose.setContent {
            IosSettingsTheme(dark = true) {
                LiquidButton({ clicks++ }, rememberIosBackdrop()) { BasicText("Готово") }
            }
        }
        compose.onNodeWithText("Готово").performClick()
        compose.onNodeWithText("Готово").performTouchInput { click(center) }
        compose.waitForIdle()
        assertEquals(2, clicks)
    }

    @Test fun blurLevelRendersAndWorks() = lowerLevelWorks(GlassLevel.BLUR)

    @Test fun fillLevelRendersAndWorks() = lowerLevelWorks(GlassLevel.FILL)

    private fun lowerLevelWorks(level: GlassLevel) {
        var clicks = 0
        val changes = mutableListOf<Boolean>()
        compose.setContent {
            IosSettingsTheme(dark = false) {
                Column {
                    var checked by remember { mutableStateOf(false) }
                    IosSwitch(checked, { checked = it; changes += it }, level = level)
                    LiquidButton({ clicks++ }, rememberIosBackdrop(), level = level) { BasicText("Изменить") }
                }
            }
        }
        compose.onNodeWithTag(IosSwitchTag).performTouchInput { click(center) }
        compose.onNodeWithText("Изменить").performTouchInput { click(center) }
        compose.waitForIdle()
        assertEquals(listOf(true), changes)
        assertEquals(1, clicks)
    }
}
