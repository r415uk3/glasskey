// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.ios

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/** Liquid switch and buttons below Android 13, where android.graphics.RuntimeShader does not exist. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30, 32])
class LiquidLowSdkTest {
    @get:Rule val compose = createComposeRule()

    @Test fun switchAndGlassButtonsWorkWithoutRuntimeShader() {
        assertNotEquals(GlassLevel.LENS, IosGlass.levelFor(Build.VERSION.SDK_INT))
        val changes = mutableListOf<Boolean>()
        var backs = 0
        compose.setContent {
            IosSettingsTheme(dark = true) {
                IosScreen("Экран", onBack = { backs++ }, action = { IosBarButton("Изменить") { } }) {
                    item {
                        Column {
                            var checked by remember { mutableStateOf(false) }
                            IosSwitch(checked, { checked = it; changes += it })
                        }
                    }
                }
            }
        }
        compose.onNodeWithTag(IosSwitchTag).performTouchInput { click(center) }
        compose.onNodeWithTag(IosBackButtonTag).performTouchInput { click(center) }
        compose.onNodeWithTag(IosBackButtonTag).performClick()
        compose.waitForIdle()
        assertEquals(listOf(true), changes)
        assertEquals(2, backs)
    }
}
