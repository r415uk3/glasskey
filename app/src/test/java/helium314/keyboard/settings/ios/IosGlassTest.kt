// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.ios

import androidx.compose.ui.graphics.Color
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class IosGlassTest {
    @Test fun lensOnAndroid13() = assertEquals(GlassLevel.LENS, IosGlass.levelFor(33))

    @Test fun blurOnAndroid12() {
        assertEquals(GlassLevel.BLUR, IosGlass.levelFor(31))
        assertEquals(GlassLevel.BLUR, IosGlass.levelFor(32))
    }

    @Test fun fillBelow12() = assertEquals(GlassLevel.FILL, IosGlass.levelFor(30))

    @Test fun darkAndLightColors() {
        assertEquals(Color(0xFF34C759), IosSettingsTheme.colors(dark = true).switchOn)
        assertEquals(Color(0xFF0A84FF), IosSettingsTheme.colors(dark = false).tint)
        assertEquals(Color.White, IosSettingsTheme.colors(dark = true).text)
        assertEquals(Color.Black, IosSettingsTheme.colors(dark = false).text)
    }
}
