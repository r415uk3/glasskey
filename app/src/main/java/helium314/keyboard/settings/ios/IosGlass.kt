// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.ios

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.geometry.Offset
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberCanvasBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.drawPlainBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight

// The only file outside liquid/ (the components ported from Kyant's catalog) that touches io.github.kyant0:backdrop;
// everything else goes through these wrappers.

enum class GlassLevel { LENS, BLUR, FILL }

object IosGlass {
    fun levelFor(sdk: Int): GlassLevel = when {
        sdk >= Build.VERSION_CODES.TIRAMISU -> GlassLevel.LENS
        sdk >= Build.VERSION_CODES.S -> GlassLevel.BLUR
        else -> GlassLevel.FILL
    }
}

typealias IosBackdrop = LayerBackdrop

@Composable
fun rememberIosBackdrop(): IosBackdrop = rememberLayerBackdrop()

/**
 * What a glass control inside a group (e.g. a switch thumb) refracts: [LocalIosBackdrop], or, outside an [IosScreen],
 * the group fill over the page background.
 */
@Composable
fun rememberGroupBackdrop(): Backdrop {
    val colors = LocalIosColors.current
    val flat = rememberCanvasBackdrop {
        drawRect(colors.background)
        drawRect(colors.groupFill)
    }
    return LocalIosBackdrop.current ?: flat
}

/** Fill drawn over the backdrop by [iosGlass]: the group fill at every [level] (the page background is plain). */
@Suppress("UNUSED_PARAMETER")
@Composable
fun iosGlassSurface(level: GlassLevel): Color = LocalIosColors.current.groupFill

/** Records this node's content so that [iosGlass] elements can refract / blur it. */
fun Modifier.iosBackdropSource(backdrop: IosBackdrop): Modifier = layerBackdrop(backdrop)

/**
 * Put directly before a Kyant `drawBackdrop` (no layer in between, so both draw into the same layer): redraws it
 * whenever this node moves on screen, e.g. while its page slides, so the glass never shows the backdrop at the
 * offset of an earlier position.
 */
fun Modifier.iosBackdropRefresh(): Modifier = this then BackdropRefreshElement

private data object BackdropRefreshElement : ModifierNodeElement<BackdropRefreshNode>() {
    override fun create() = BackdropRefreshNode()
    override fun update(node: BackdropRefreshNode) {}
}

private class BackdropRefreshNode : Modifier.Node(), DrawModifierNode, GlobalPositionAwareModifierNode {
    private var coordinates: LayoutCoordinates? = null
    private var drawnAt = Offset.Unspecified

    private fun position(): Offset = coordinates?.takeIf { it.isAttached }?.positionInRoot() ?: Offset.Unspecified

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        this.coordinates = coordinates
        if (position() != drawnAt) invalidateDraw()
    }

    override fun ContentDrawScope.draw() {
        drawnAt = position()
        IosBackdropProbe.onDraw?.invoke(this@BackdropRefreshNode, drawnAt) { position() }
        drawContent()
    }

    override fun onDetach() {
        coordinates = null
        drawnAt = Offset.Unspecified
    }
}

/** Test hook: called whenever glass is drawn, with where it was drawn and a way to ask where it is now. */
@VisibleForTesting
object IosBackdropProbe {
    var onDraw: ((node: Any, drawnAt: Offset, current: () -> Offset) -> Unit)? = null
}

/**
 * Draws [backdrop] (the shared settings background) behind this node's content exactly where it is on screen, so
 * a page that slides over another hides it while the background itself stays still.
 */
fun Modifier.iosWallpaper(backdrop: IosBackdrop): Modifier = this then WallpaperElement(backdrop)

private data class WallpaperElement(val backdrop: IosBackdrop) : ModifierNodeElement<WallpaperNode>() {
    override fun create() = WallpaperNode(backdrop)
    override fun update(node: WallpaperNode) {
        node.backdrop = backdrop
        node.invalidateDraw()
    }
}

private class WallpaperNode(var backdrop: IosBackdrop) : Modifier.Node(), DrawModifierNode, GlobalPositionAwareModifierNode {
    private var coordinates: LayoutCoordinates? = null

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        this.coordinates = coordinates
        invalidateDraw() // the offset to the background changes while the page slides
    }

    override fun ContentDrawScope.draw() {
        // the background is drawn shifted to where it is on screen; only this page's own area may show it, or a
        // page sliding in would paint it over the page it is covering
        clipRect { with(backdrop) { drawBackdrop(this@draw, coordinates) } }
        drawContent()
    }

    override fun onDetach() {
        coordinates = null
    }
}

@Composable
fun Modifier.iosGlass(
    backdrop: IosBackdrop,
    shape: Shape,
    level: GlassLevel = IosGlass.levelFor(Build.VERSION.SDK_INT),
): Modifier {
    val fill = LocalIosColors.current.groupFill
    val dark = LocalIosDark.current
    return when (level) {
        GlassLevel.LENS -> iosBackdropRefresh().drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                vibrancy()
                blur(2.dp.toPx())
                // the library's lens supports rounded corner shapes only
                if (shape is CornerBasedShape) lens(12.dp.toPx(), 24.dp.toPx())
            },
            highlight = { Highlight.Default },
            onDrawSurface = { drawRect(fill) },
        )
        GlassLevel.BLUR -> iosBackdropRefresh().drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                vibrancy()
                blur(20.dp.toPx())
            },
            highlight = { Highlight.Default },
            onDrawSurface = { drawRect(fill) },
        )
        GlassLevel.FILL -> background(fill, shape)
            .border(0.5.dp, Color.White.copy(alpha = if (dark) 0.22f else 0.6f), shape)
    }
}

/**
 * iOS scroll-edge effect for a top bar: frosted (or, below Android 12, nearly opaque) background colour that fades
 * out over the last [fade] of the bar instead of ending in a hard edge or rim line.
 */
@Composable
fun Modifier.iosScrollEdge(
    backdrop: IosBackdrop,
    fade: Dp,
    level: GlassLevel = IosGlass.levelFor(Build.VERSION.SDK_INT),
): Modifier {
    val background = LocalIosColors.current.background
    val dark = LocalIosDark.current
    val masked = graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val start = IosScrollEdge.fadeStart(size.height, fade.toPx())
            drawRect(
                Brush.verticalGradient(0f to Color.Black, start to Color.Black, 1f to Color.Transparent),
                blendMode = BlendMode.DstIn,
            )
        }
    return when (level) {
        GlassLevel.LENS, GlassLevel.BLUR -> {
            val tint = background.copy(alpha = if (dark) 0.55f else 0.65f)
            masked.iosBackdropRefresh().drawPlainBackdrop(
                backdrop = backdrop,
                shape = { RectangleShape },
                effects = {
                    vibrancy()
                    blur(16.dp.toPx())
                },
                onDrawSurface = { drawRect(tint) },
            )
        }
        GlassLevel.FILL -> masked.background(background.copy(alpha = 0.94f))
    }
}
