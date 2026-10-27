// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.ios

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import helium314.keyboard.settings.SettingsActivity
import helium314.keyboard.settings.SettingsContainer
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Android 12: blur glass (Kyant drawBackdrop); the window is rendered by hand each frame
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "ru-w402dp-h900dp", sdk = [32])
class IosGlassRefreshTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    // hardware rendering: only invalidated layers are re-recorded, as on a device
    private fun drawWindow() {
        compose.runOnUiThread {
            val v = compose.activity.window.decorView
            val b = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
            android.view.PixelCopy.request(compose.activity.window, b, {}, android.os.Handler(android.os.Looper.getMainLooper()))
        }
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
    }

    companion object {
        init { System.setProperty("robolectric.screenshot.hwrdr.native", "true") }
    }

    // glass inside a sliding page must be redrawn at its final position, or it shows the still background offset
    @Test fun glassIsRedrawnWhereItSettles() {
        SettingsActivity.settingsContainer = SettingsContainer(compose.activity)
        val draws = HashMap<Any, Pair<Offset, () -> Offset>>()
        IosBackdropProbe.onDraw = { node, at, current -> draws[node] = at to current }
        try {
            compose.setContent { IosSettingsTheme(dark = true) { IosSettingsNavHost({}, false, {}, 1f) } }
            compose.waitForIdle()
            drawWindow()
            compose.mainClock.autoAdvance = false
            compose.onNodeWithText("Клавиатуры").performClick()
            repeat(40) {
                compose.mainClock.advanceTimeByFrame()
                compose.waitForIdle()
                drawWindow()
            }
            compose.mainClock.autoAdvance = true
            compose.waitForIdle()
            drawWindow()
            // the harness only redraws what was invalidated (else this test could not fail)
            var idleDraws = 0
            IosBackdropProbe.onDraw = { _, _, _ -> idleDraws++ }
            drawWindow()
            assertEquals(0, idleDraws)
            val live = draws.values.mapNotNull { (at, current) -> current().takeIf { it.isSpecified }?.let { at to it } }
            assertTrue(live.size >= 3, "glass drawn on the new page: ${live.size}")
            live.forEach { (at, now) -> assertTrue((at - now).getDistance() < 1f, "drawn at $at, now at $now") }
        } finally {
            IosBackdropProbe.onDraw = null
        }
    }
}
