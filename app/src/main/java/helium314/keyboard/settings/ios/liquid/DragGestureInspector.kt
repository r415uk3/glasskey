// Ported from Kyant0/AndroidLiquidGlass catalog @65ab177 (Apache-2.0), Copyright Kyant.
// Modifications (Glasskey): the catalog's `inspectDragGestures` helper was not among the copied sources; this is a
// reimplementation with the same contract (report a press and every movement of the first pointer, never consume, so
// clickable / scrollable modifiers on the same node keep working), plus [awaitSwitchDragGestures] for the toggle,
// which claims the pointer only after horizontal movement so that a vertical drag scrolls the list instead.
package helium314.keyboard.settings.ios.liquid

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.isOutOfBounds
import androidx.compose.ui.input.pointer.positionChangeIgnoreConsumed
import kotlinx.coroutines.CancellationException
import kotlin.math.abs

/** Observes press, movement and release of the first pointer without consuming anything. */
suspend fun PointerInputScope.inspectDragGestures(
    onDragStart: (down: PointerInputChange) -> Unit = {},
    onDragEnd: (change: PointerInputChange) -> Unit = {},
    onDragCancel: () -> Unit = {},
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit,
) = awaitEachGesture {
    val down = awaitFirstDown(requireUnconsumed = false)
    onDragStart(down)
    try {
        while (true) {
            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
            if (change == null) {
                onDragCancel()
                break
            }
            if (change.changedToUpIgnoreConsumed()) {
                onDragEnd(change)
                break
            }
            val dragAmount = change.positionChangeIgnoreConsumed()
            if (dragAmount != Offset.Zero) onDrag(change, dragAmount)
        }
    } catch (e: CancellationException) {
        onDragCancel()
        throw e
    }
}

/**
 * Switch gestures: [onPress] on down; a release before any movement is a tap ([onRelease] with `dragged = false`);
 * horizontal movement past the touch slop claims the pointer, reports the whole offset since the down once and then
 * every further movement to [onDrag], and ends in [onRelease] with `dragged = true`. Vertical movement first, leaving the
 * bounds before moving, or another node consuming the events ends the gesture in [onCancel] and leaves the events to
 * the list.
 */
suspend fun PointerInputScope.awaitSwitchDragGestures(
    onPress: (down: PointerInputChange) -> Unit,
    onRelease: (dragged: Boolean) -> Unit,
    onCancel: () -> Unit,
    onDrag: (dragAmount: Offset) -> Unit,
) = awaitEachGesture {
    val down = awaitFirstDown()
    down.consume()
    onPress(down)
    var claimed = false
    var finished = false
    try {
        while (true) {
            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
            if (change.isConsumed) break // someone else (e.g. a scroll) took the gesture
            if (!change.pressed) {
                change.consume()
                finished = true
                onRelease(claimed)
                break
            }
            if (!claimed) {
                val delta = change.position - down.position
                val slop = viewConfiguration.touchSlop
                // vertical movement first: leave the events unconsumed so the list scrolls
                if (abs(delta.y) > slop && abs(delta.y) >= abs(delta.x)) break
                if (abs(delta.x) > slop) {
                    claimed = true
                    change.consume()
                    onDrag(delta)
                } else if (change.isOutOfBounds(size, extendedTouchPadding)) {
                    break
                }
            } else {
                change.consume()
                val dragAmount = change.positionChangeIgnoreConsumed()
                if (dragAmount != Offset.Zero) onDrag(dragAmount)
            }
        }
    } finally {
        if (!finished) onCancel()
    }
}
