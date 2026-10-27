// Ported from Kyant0/AndroidLiquidGlass catalog @65ab177 (catalog/.../components/LiquidButton.kt), Apache-2.0,
// Copyright Kyant. Modifications (Glasskey):
// - package; `com.kyant.shapes.Capsule` (not in backdrop 1.0.1) -> [Capsule], a RoundedCornerShape(50) (circular, not
//   G2-continuous, corners);
// - new [level] parameter: GlassLevel.LENS draws the catalog's vibrancy + blur + lens; BLUR (Android 12) drops the lens
//   and blurs more over a translucent fill; FILL (< 12) draws no backdrop at all, just a translucent fill and a thin rim;
//   the InteractiveHighlight spot-light shader is used at LENS only;
// - new [height] / [horizontalPadding] parameters (catalog: 48dp / 16dp) so that 44dp circular bar buttons fit;
// - the glass surface is [liquidButtonSurface], shared with the floating search field.
package helium314.keyboard.settings.ios.liquid

import android.os.Build
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import helium314.keyboard.settings.ios.GlassLevel
import helium314.keyboard.settings.ios.IosGlass
import helium314.keyboard.settings.ios.LocalIosColors
import helium314.keyboard.settings.ios.LocalIosDark
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

/** Stand-in for `com.kyant.shapes.Capsule`. */
fun Capsule(): Shape = capsule

private val capsule = RoundedCornerShape(percent = 50)

@Composable
fun LiquidButton(
    onClick: () -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    isInteractive: Boolean = true,
    tint: Color = Color.Unspecified,
    surfaceColor: Color = Color.Unspecified,
    level: GlassLevel = IosGlass.levelFor(Build.VERSION.SDK_INT),
    height: Dp = 48f.dp,
    horizontalPadding: Dp = 16f.dp,
    content: @Composable RowScope.() -> Unit
) {
    val interactiveHighlight = rememberInteractiveHighlight(level)

    Row(
        modifier
            .liquidButtonSurface(
                backdrop = backdrop,
                shape = { Capsule() },
                level = level,
                interactiveHighlight = if (isInteractive) interactiveHighlight else null,
                tint = tint,
                surfaceColor = surfaceColor,
            )
            .clickable(
                interactionSource = null,
                indication = if (isInteractive) null else LocalIndication.current,
                role = Role.Button,
                onClick = onClick
            )
            .then(
                if (isInteractive) {
                    Modifier
                        .then(interactiveHighlight.modifier)
                        .then(interactiveHighlight.gestureModifier)
                } else {
                    Modifier
                }
            )
            .height(height)
            .padding(horizontal = horizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(8f.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
fun rememberInteractiveHighlight(level: GlassLevel): InteractiveHighlight {
    val animationScope = rememberCoroutineScope()
    return remember(animationScope, level) {
        InteractiveHighlight(
            animationScope = animationScope,
            useShader = level == GlassLevel.LENS
        )
    }
}

/**
 * LiquidButton's glass surface and, with an [interactiveHighlight], its press / drag deformation. Add
 * `interactiveHighlight.modifier` and `.gestureModifier` after it for the highlight and the gesture tracking.
 */
@Composable
fun Modifier.liquidButtonSurface(
    backdrop: Backdrop,
    shape: () -> Shape,
    level: GlassLevel,
    interactiveHighlight: InteractiveHighlight?,
    tint: Color = Color.Unspecified,
    surfaceColor: Color = Color.Unspecified,
): Modifier {
    val fill = LocalIosColors.current.groupFill
    val dark = LocalIosDark.current
    val layerBlock: (GraphicsLayerScope.() -> Unit)? = if (interactiveHighlight != null) {
        {
            val width = size.width
            val height = size.height

            val progress = interactiveHighlight.pressProgress
            val scale = lerp(1f, 1f + 4f.dp.toPx() / size.height, progress)

            val maxOffset = size.minDimension
            val initialDerivative = 0.05f
            val offset = interactiveHighlight.offset
            translationX = maxOffset * tanh(initialDerivative * offset.x / maxOffset)
            translationY = maxOffset * tanh(initialDerivative * offset.y / maxOffset)

            val maxDragScale = 4f.dp.toPx() / size.height
            val offsetAngle = atan2(offset.y, offset.x)
            scaleX =
                scale +
                        maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension) *
                        (width / height).fastCoerceAtMost(1f)
            scaleY =
                scale +
                        maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension) *
                        (height / width).fastCoerceAtMost(1f)
        }
    } else {
        null
    }
    val onDrawSurface: DrawScope.() -> Unit = {
        if (tint.isSpecified) {
            drawRect(tint, blendMode = BlendMode.Hue)
            drawRect(tint.copy(alpha = 0.75f))
        }
        if (surfaceColor.isSpecified) {
            drawRect(surfaceColor)
        }
    }
    return when (level) {
        GlassLevel.LENS -> drawBackdrop(
            backdrop = backdrop,
            shape = shape,
            effects = {
                vibrancy()
                blur(2f.dp.toPx())
                lens(12f.dp.toPx(), 24f.dp.toPx())
            },
            layerBlock = layerBlock,
            onDrawSurface = onDrawSurface
        )
        GlassLevel.BLUR -> drawBackdrop(
            backdrop = backdrop,
            shape = shape,
            effects = {
                vibrancy()
                blur(20f.dp.toPx())
            },
            layerBlock = layerBlock,
            onDrawSurface = {
                onDrawSurface()
                if (!tint.isSpecified && !surfaceColor.isSpecified) drawRect(fill)
            }
        )
        GlassLevel.FILL -> {
            val s = shape()
            // clipped like drawBackdrop's layer, so the press highlight stays inside the shape
            val base = graphicsLayer {
                layerBlock?.invoke(this)
                this.shape = s
                clip = true
            }
                .background(surfaceColor.takeOrElse { fill }, s)
            (if (tint.isSpecified) base.background(tint.copy(alpha = 0.75f), s) else base)
                .border(0.5f.dp, Color.White.copy(alpha = if (dark) 0.22f else 0.6f), s)
        }
    }
}
