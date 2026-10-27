// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.ios

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import helium314.keyboard.settings.SettingsActivity
import helium314.keyboard.settings.SettingsContainer
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper
import kotlin.test.Test
import kotlin.test.assertTrue

/** Rendered (hardware rendering, Android 16, lens glass) frames of a push, light theme. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-w400dp-h800dp")
class IosNavRenderTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    companion object {
        init { System.setProperty("robolectric.screenshot.hwrdr.native", "true") }
    }

    private fun capture(): Bitmap {
        val v = compose.activity.window.decorView
        val b = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
        compose.runOnUiThread { PixelCopy.request(compose.activity.window, b, {}, Handler(Looper.getMainLooper())) }
        ShadowLooper.idleMainLooper()
        return b
    }

    // the incoming page shows the shared background only within its own bounds: the old page stays visible beside it
    @Test fun oldPageStaysVisibleWhileTheNewOneSlidesIn() {
        SettingsActivity.settingsContainer = SettingsContainer(compose.activity)
        compose.setContent { IosSettingsTheme(dark = false) { IosSettingsNavHost({}, false, {}, 1f) } }
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        compose.onNodeWithText("Keyboards").performClick()
        compose.mainClock.advanceTimeBy(100)
        compose.waitForIdle()
        val frame = capture()
        val title = compose.onAllNodesWithTag(IosLargeTitleTag).fetchSemanticsNodes()
            .first { it.config[SemanticsProperties.Text].joinToString() == "Glasskey" }.boundsInWindow
        val incoming = compose.onAllNodesWithTag(IosLargeTitleTag).fetchSemanticsNodes()
            .first { it.config[SemanticsProperties.Text].joinToString() == "Keyboards" }.boundsInWindow.left
        // black title text of the old page, left of the incoming page
        var dark = 0
        for (x in maxOf(0, title.left.toInt())..<minOf(incoming.toInt(), title.right.toInt()))
            for (y in title.top.toInt()..<title.bottom.toInt())
                if (Color(frame.getPixel(x, y)).luminance() < 0.2f) dark++
        assertTrue(dark > 20, "old title pixels visible: $dark (title $title, incoming at $incoming)")
        compose.mainClock.autoAdvance = true
    }
}
