// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.ios

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

const val IosBackgroundTag = "ios_background"

/** Plain iOS grouped background behind the settings glass (black in dark mode, #F2F2F7 in light mode). */
@Composable
fun IosBackground(dark: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier.testTag(IosBackgroundTag)) {
        drawRect(IosSettingsTheme.colors(dark).background)
    }
}
