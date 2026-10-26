// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.ios

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.isOutOfBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt

private val trackWidth = 52.dp
private val trackHeight = 31.dp
private val thumbInset = 2.dp
private val thumbWidth = 38.dp
private val thumbHeight = 27.dp
private val pressedThumbWidth = 46.dp
private val pressedThumbHeight = 35.dp
private val thumbTravel = trackWidth - thumbInset * 2 - thumbWidth
private val pill = RoundedCornerShape(percent = 50)

const val IosSwitchTag = "ios_switch"

/** iOS pill switch; while pressed the thumb grows into a glass lens over the track. */
@Composable
fun IosSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    level: GlassLevel = IosGlass.levelFor(Build.VERSION.SDK_INT),
) {
    val colors = LocalIosColors.current
    val onChange by rememberUpdatedState(onCheckedChange)
    var pressed by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    val fraction by animateFloatAsState(if (dragging) dragFraction else if (checked) 1f else 0f, label = "thumb")
    val track by animateColorAsState(if (checked) colors.switchOn else colors.switchOff, label = "track")
    val thumbW by animateDpAsState(if (pressed) pressedThumbWidth else thumbWidth, label = "thumbW")
    val thumbH by animateDpAsState(if (pressed) pressedThumbHeight else thumbHeight, label = "thumbH")
    val travelPx = with(LocalDensity.current) { thumbTravel.toPx() }
    // the thumb refracts the track, so the track is its own backdrop
    val backdrop = rememberIosBackdrop()

    Box(
        modifier
            .testTag(IosSwitchTag)
            .size(trackWidth, trackHeight)
            .semantics {
                role = Role.Switch
                toggleableState = ToggleableState(checked)
                onClick { onChange(!checked); true }
            }
            .pointerInput(checked) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    pressed = true
                    try {
                        val start = if (checked) 1f else 0f
                        var frac = start
                        var moved = false
                        var released = false
                        while (true) {
                            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                            if (change.isConsumed) break // someone else (e.g. a scroll) took the gesture
                            if (!change.pressed) {
                                change.consume()
                                released = true
                                break
                            }
                            val delta = change.position - down.position
                            if (!moved) {
                                val slop = viewConfiguration.touchSlop
                                // vertical movement first: leave the events unconsumed so the list scrolls
                                if (abs(delta.y) > slop && abs(delta.y) >= abs(delta.x)) break
                                if (abs(delta.x) > slop) {
                                    moved = true
                                    dragging = true
                                } else if (change.isOutOfBounds(size, extendedTouchPadding)) {
                                    break
                                }
                            }
                            if (moved) {
                                change.consume()
                                frac = (start + delta.x / travelPx).coerceIn(0f, 1f)
                                dragFraction = frac
                            }
                        }
                        if (released) {
                            val result = IosSwitchLogic.checkedAfterRelease(checked, frac, moved)
                            if (result != checked) onChange(result)
                        }
                    } finally {
                        pressed = false
                        dragging = false
                    }
                }
            }
    ) {
        Box(Modifier.fillMaxSize().iosBackdropSource(backdrop).background(track, pill))
        val thumb = Modifier
            .wrapContentSize(Alignment.TopStart, unbounded = true)
            .offset {
                // keep the thumb centred on its resting centre while it grows
                val cx = (thumbInset + thumbWidth / 2).toPx() + fraction * travelPx
                val cy = trackHeight.toPx() / 2
                IntOffset((cx - thumbW.toPx() / 2).roundToInt(), (cy - thumbH.toPx() / 2).roundToInt())
            }
            .size(thumbW, thumbH)
        if (pressed) {
            Box(thumb.iosGlass(backdrop, pill, level).border(1.dp, Color.White.copy(alpha = 0.7f), pill))
        } else {
            Box(thumb.shadow(3.dp, pill).background(Color.White, pill))
        }
    }
}
