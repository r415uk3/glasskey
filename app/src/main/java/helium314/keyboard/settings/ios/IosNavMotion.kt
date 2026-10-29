// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.ios

import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.VectorizedFiniteAnimationSpec
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.unit.dp
import kotlin.math.exp
import kotlin.math.min
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
     * Longest a push / pop waits at its start for the incoming page's first frame (see [IosNavGate]); a page that
     * takes longer starts moving anyway. Not scaled by the animation scale: it is waiting, not motion.
     */
    const val MaxHoldMillis = 500
}

/**
 * Where a push / pop starts on screen. The transition's clock starts on the frame in which the incoming page is
 * first composed, and that page's first composition, layout and draw can take 100-250 ms; the next frame would then
 * already be that far into the curve, so the first half of the slide would never be seen. The gate holds the
 * transition at its start until the incoming page has been drawn once, and the curve starts from the first frame
 * after that ([localPlayTimeNanos]).
 */
class IosNavGate(
    private val onStart: (holdNanos: Long) -> Unit = {},
    private val onEvaluate: (playTimeNanos: Long, localNanos: Long, open: Boolean) -> Unit = { _, _, _ -> }, // TEMPORARY diagnostics
) {
    var isOpen = false
        private set
    private var holdNanos = -1L
    private var lastPlayTimeNanos = -1L // latest play time the slide was evaluated at
    private var openedAfterNanos = -1L // play time of the frame in which the gate opened

    /**
     * The incoming page has been laid out (and is drawn in this same frame): the slide starts on the next frame.
     * The page is laid out at the earliest in the transition's first frame, at play time 0.
     */
    fun open() {
        if (isOpen) return
        isOpen = true
        openedAfterNanos = maxOf(lastPlayTimeNanos, 0L)
    }

    /** The incoming page is already shown (e.g. back during a push): the slide starts right away. */
    fun openNow() {
        if (isOpen) return
        isOpen = true
        openedAfterNanos = -1L
    }

    /**
     * Play time of the motion itself for the transition's [playTimeNanos]: 0 until the first frame after [open],
     * then counting from that frame; at most [maxHoldNanos] are held back.
     *
     * The slide is evaluated again in the frame in which the page is laid out (the layout pass sets the slide's
     * offsets), at that frame's play time; only a later play time is a new frame.
     */
    fun localPlayTimeNanos(playTimeNanos: Long, maxHoldNanos: Long): Long {
        if (playTimeNanos < maxHoldNanos) lastPlayTimeNanos = maxOf(lastPlayTimeNanos, playTimeNanos)
        if (holdNanos < 0 && isOpen && (playTimeNanos > openedAfterNanos || playTimeNanos >= maxHoldNanos)) {
            holdNanos = min(playTimeNanos, maxHoldNanos)
            onStart(holdNanos)
        }
        val hold = if (holdNanos >= 0) holdNanos else min(playTimeNanos, maxHoldNanos)
        onEvaluate(playTimeNanos, playTimeNanos - hold, isOpen)
        return playTimeNanos - hold
    }
}

/**
 * [motion] held at its start by [gate] (see [IosNavGate]); lasts [IosNavMotion.MaxHoldMillis] longer than [motion]
 * so the whole curve always fits, the remaining time the pages just stay where they settled.
 */
class IosHeldSpec<T>(
    val gate: IosNavGate,
    val motion: FiniteAnimationSpec<T>,
    val maxHoldMillis: Int = IosNavMotion.MaxHoldMillis,
) : FiniteAnimationSpec<T> {
    override fun <V : AnimationVector> vectorize(converter: TwoWayConverter<T, V>): VectorizedFiniteAnimationSpec<V> =
        Held(gate, motion.vectorize(converter), maxHoldMillis * 1_000_000L)

    private class Held<V : AnimationVector>(
        val gate: IosNavGate,
        val motion: VectorizedFiniteAnimationSpec<V>,
        val maxHoldNanos: Long,
    ) : VectorizedFiniteAnimationSpec<V> {
        private fun local(playTimeNanos: Long) = gate.localPlayTimeNanos(playTimeNanos, maxHoldNanos)

        override fun getValueFromNanos(playTimeNanos: Long, initialValue: V, targetValue: V, initialVelocity: V): V =
            motion.getValueFromNanos(local(playTimeNanos), initialValue, targetValue, initialVelocity)

        override fun getVelocityFromNanos(playTimeNanos: Long, initialValue: V, targetValue: V, initialVelocity: V): V =
            motion.getVelocityFromNanos(local(playTimeNanos), initialValue, targetValue, initialVelocity)

        override fun getDurationNanos(initialValue: V, targetValue: V, initialVelocity: V): Long =
            motion.getDurationNanos(initialValue, targetValue, initialVelocity) + maxHoldNanos
    }
}

/**
 * The gates of the push / pop transitions in one navigation host: one gate for each transition, opened when its
 * incoming page has been laid out and drawn ([iosNavPage] reports that).
 */
class IosNavClock {
    private var current: IosNavGate? = null
    private var currentKey: Pair<String, String>? = null
    private var currentTarget: String? = null
    private val drawnPages = mutableMapOf<String, Int>() // page (back stack entry id) -> drawn page nodes

    /** Called when a push / pop starts to move, with how long it was held at its start. */
    var onSlideStart: (holdNanos: Long) -> Unit = {}
    var onGateEvaluate: (playTimeNanos: Long, localNanos: Long, open: Boolean) -> Unit = { _, _, _ -> } // TEMPORARY
    var onPagePlaced: (page: String) -> Unit = {} // TEMPORARY

    /**
     * The gate of the push / pop from page [initial] to page [target]. AnimatedContent asks for the transition
     * more than once (the entering and the leaving page each ask for it), so all asks for the same pair get the
     * same gate; back stack entries are new for every push, so the same pair does not come back for a later one.
     * A gate for a page that is already shown (e.g. back during a push) is open at once.
     */
    fun gate(initial: String, target: String): IosNavGate {
        val key = initial to target
        current?.let { if (currentKey == key) return it }
        val gate = IosNavGate({ onSlideStart(it) }, { p, l, o -> onGateEvaluate(p, l, o) })
        if ((drawnPages[target] ?: 0) > 0) gate.openNow()
        current = gate
        currentKey = key
        currentTarget = target
        return gate
    }

    internal fun onPageDrawn(page: String) {
        onPagePlaced(page)
        drawnPages[page] = (drawnPages[page] ?: 0) + 1
        if (page == currentTarget) current?.open()
    }

    internal fun onPageGone(page: String) {
        val n = (drawnPages[page] ?: 0) - 1
        if (n > 0) drawnPages[page] = n else drawnPages.remove(page)
    }
}

/** Tells [clock] when this page (back stack entry [page]) has been laid out and drawn for the first time. */
internal fun Modifier.iosNavPage(clock: IosNavClock, page: String): Modifier = this then NavPageElement(clock, page)

private data class NavPageElement(val clock: IosNavClock, val page: String) : ModifierNodeElement<NavPageNode>() {
    override fun create() = NavPageNode(clock, page)
    override fun update(node: NavPageNode) {
        if (node.clock === clock && node.page == page) return
        node.forget()
        node.clock = clock
        node.page = page
    }
}

// placed rather than drawn: a page is drawn in the same frame right after it is placed, and placement is also
// seen where nothing is drawn (Robolectric with a paused clock)
private class NavPageNode(var clock: IosNavClock, var page: String) : Modifier.Node(), LayoutAwareModifierNode {
    private var drawn = false

    override fun onPlaced(coordinates: LayoutCoordinates) {
        if (!drawn) {
            drawn = true
            clock.onPageDrawn(page)
        }
    }

    fun forget() {
        if (drawn) clock.onPageGone(page)
        drawn = false
    }

    override fun onDetach() = forget()
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
            IosNavDiagnostics.pageOffset(label, fraction) // TEMPORARY, see IosNavDiagnostics
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
