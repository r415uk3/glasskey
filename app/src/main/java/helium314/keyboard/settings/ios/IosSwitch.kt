// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.ios

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import helium314.keyboard.settings.ios.liquid.LiquidToggle

const val IosSwitchTag = "ios_switch"

/**
 * iOS 27 switch: Kyant's LiquidToggle (64x28dp track; while pressed or dragged the thumb grows into a glass lens that
 * refracts the group behind it and the track). A tap toggles, a horizontal drag settles on the nearer side, a vertical
 * drag that starts on the switch scrolls the list.
 */
@Composable
fun IosSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    level: GlassLevel = IosGlass.levelFor(Build.VERSION.SDK_INT),
) {
    val onChange by rememberUpdatedState(onCheckedChange)
    val checkedState = rememberUpdatedState(checked)
    val selected = remember { { checkedState.value } }
    LiquidToggle(
        selected = selected,
        // the toggle reports every settled gesture, also a drag towards the side it already is on
        onSelect = { if (it != checkedState.value) onChange(it) },
        backdrop = rememberGroupBackdrop(),
        modifier = modifier
            .testTag(IosSwitchTag)
            .semantics {
                role = Role.Switch
                toggleableState = ToggleableState(checked)
                onClick { onChange(!checked); true }
            },
        level = level,
    )
}
