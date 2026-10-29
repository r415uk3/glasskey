// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.ios

import androidx.activity.BackEventCompat
import androidx.compose.animation.core.Easing
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.unit.dp
import kotlin.math.exp
import kotlin.math.roundToInt

/** iOS navigation push / pop: timing, and how much the page underneath dims and the page on top casts a shadow. */
object IosNavMotion {
    const val DurationMillis = 450

    /** How far toward the leading edge the covered page moves, as a fraction of the width. */
    const val ParallaxFraction = 0.3f

    private const val MaxDimLight = 0.12f
    private const val MaxDimDark = 0.25f
    const val MaxShadowAlpha = 0.18f

    fun durationMillis(animationScale: Float): Int = (DurationMillis * animationScale).roundToInt().coerceAtLeast(0)

    // a critically damped spring (no bounce) as a fixed-duration easing, so that it can be scaled and seeked:
    // 1 - (1 + wt) e^(-wt), normalised to reach exactly 1 at the end
    private const val OMEGA = 6.6f
    private val end = 1f - (1f + OMEGA) * exp(-OMEGA)
    val Easing = Easing { x -> if (x >= 1f) 1f else (1f - (1f + OMEGA * x) * exp(-OMEGA * x)) / end }

    /**
     * Black overlay on a page whose leading edge is at [offsetFraction] of the width (in layout direction, negative
     * toward the leading edge): only the covered page, pushed toward the leading edge, is dimmed.
     */
    fun dimAlpha(offsetFraction: Float, dark: Boolean): Float {
        if (offsetFraction >= 0f) return 0f
        val max = if (dark) MaxDimDark else MaxDimLight
        return (-offsetFraction / ParallaxFraction).coerceIn(0f, 1f) * max
    }

    /** Strength (0..1) of the shadow on the leading edge of the page on top, fading out as it settles. */
    fun shadowAlpha(offsetFraction: Float): Float = offsetFraction.coerceIn(0f, 1f)

    /**
     * Where a back gesture puts the top page (0 in place, 1 gone): from the leading edge it follows the finger, the
     * page's edge under the finger as on iOS; from the other edge (no iOS equivalent) the system's progress.
     */
    /** How long nothing must happen before a page is prepared ahead of time. */
    const val PrewarmIdleMillis = 400L

    /**
     * Pages prepared ahead of time on [route], so that pushing them starts moving on the next frame: the plain
     * pages behind its rows. Not those that read lists or files once when composed (adding a keyboard,
     * dictionaries), which could be out of date by the time they are pushed.
     */
    fun prewarmRoutes(route: String): List<String> = when (route) {
        IosRoute.Main -> listOf(IosRoute.Keyboards, IosRoute.TextReplacement, IosRoute.Appearance, IosRoute.About)
        IosRoute.About -> listOf(IosRoute.Licenses)
        else -> emptyList()
    }

    fun gestureFraction(event: BackEventCompat, width: Float, rtl: Boolean): Float {
        val leading = if (rtl) BackEventCompat.EDGE_RIGHT else BackEventCompat.EDGE_LEFT
        if (event.swipeEdge != leading || width <= 0f) return event.progress
        return ((if (rtl) width - event.touchX else event.touchX) / width).coerceIn(0f, 1f)
    }
}

/** Timing of the iOS alert / sheet card: fades in while scaling up from [InitialScale], the reverse on dismiss. */
object IosSheetMotion {
    const val DurationMillis = 250
    const val InitialScale = 0.9f
}

/**
 * Dims this page while it is covered by a page sliding over it and draws a soft shadow on its leading edge while it
 * slides over another one, from where it is relative to [host] (the container both pages move in).
 */
internal fun Modifier.iosNavOverlay(host: () -> LayoutCoordinates?, dark: Boolean, rtl: Boolean, label: String): Modifier =
    this then NavOverlayElement(host, dark, rtl, label)

private data class NavOverlayElement(
    val host: () -> LayoutCoordinates?,
    val dark: Boolean,
    val rtl: Boolean,
    val label: String,
) : ModifierNodeElement<NavOverlayNode>() {
    override fun create() = NavOverlayNode(host, dark, rtl, label)
    override fun update(node: NavOverlayNode) {
        node.host = host
        node.dark = dark
        node.rtl = rtl
        node.label = label
        node.invalidateDraw()
    }
}

private class NavOverlayNode(
    var host: () -> LayoutCoordinates?,
    var dark: Boolean,
    var rtl: Boolean,
    var label: String,
) : Modifier.Node(), DrawModifierNode, GlobalPositionAwareModifierNode {
    private var offsetFraction = 0f

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        val host = host()
        val width = coordinates.size.width
        val fraction = if (host == null || !host.isAttached || width == 0) 0f else {
            val x = host.localPositionOf(coordinates, Offset.Zero).x / width
            if (rtl) -x else x
        }
        if (fraction != offsetFraction) {
            offsetFraction = fraction
            invalidateDraw()
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        val dim = IosNavMotion.dimAlpha(offsetFraction, dark)
        if (dim > 0f) drawRect(Color.Black.copy(alpha = dim))
        val shadow = IosNavMotion.shadowAlpha(offsetFraction) * IosNavMotion.MaxShadowAlpha
        if (shadow > 0f) {
            // outside the page, on the page underneath
            val w = 24.dp.toPx()
            val edge = if (rtl) size.width else 0f
            val outer = if (rtl) size.width + w else -w
            drawRect(
                Brush.horizontalGradient(
                    0f to Color.Black.copy(alpha = shadow),
                    0.35f to Color.Black.copy(alpha = shadow * 0.35f),
                    1f to Color.Transparent,
                    startX = edge,
                    endX = outer,
                ),
                topLeft = Offset(minOf(edge, outer), 0f),
                size = Size(w, size.height),
            )
        }
    }
}
