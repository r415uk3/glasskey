// Ported from Kyant0/AndroidLiquidGlass catalog @65ab177 (catalog/.../components/LiquidToggle.kt), Apache-2.0,
// Copyright Kyant. Modifications (Glasskey):
// - package; `com.kyant.shapes.Capsule` -> [Capsule] (RoundedCornerShape(50));
// - colours from LocalIosColors (switchOn #34C759 light / #30D158 dark, switchOff track) instead of
//   isSystemInDarkTheme() and the catalog's literals;
// - the drag / tap gesture ([DampedDragAnimation.modifier]) is on the whole switch instead of only the thumb, so a tap
//   anywhere on the track toggles, as on iOS; it claims the pointer only after horizontal movement (vertical drags
//   scroll the list) and an abandoned gesture snaps back without calling [onSelect];
// - a released drag settles by the dragged `fraction` instead of the animation's `targetValue` (same value once the
//   animation has caught up, but not dependent on frame timing);
// - the once-created gesture callbacks read [selected] / [onSelect] through rememberUpdatedState;
// - no `role = Role.Switch` on the thumb: the caller (IosSwitch) owns the semantics (role, state, click action);
// - new [level] parameter: GlassLevel.LENS is the catalog's thumb as is; BLUR (Android 12) drops the lens and keeps a
//   frosted, mostly white thumb; FILL (< 12) draws a plain white thumb with a shadow and no backdrop at all;
// - at LENS and BLUR the thumb is glass only while pressed or dragged: at rest the catalog's thumb is an opaque white
//   capsule with a faint shadow (its blur, lens, highlight and inner shadow are all hidden or zero), so it is drawn
//   as exactly that, without a backdrop layer, shaders or a track layer. Seven resting switches on a page then cost
//   nothing extra in a page's first frame and are not re-rendered on every frame of a push / pop.
package helium314.keyboard.settings.ios.liquid

import android.graphics.BlurMaskFilter
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import helium314.keyboard.settings.ios.iosBackdropRefresh
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import helium314.keyboard.settings.ios.GlassLevel
import helium314.keyboard.settings.ios.IosGlass
import helium314.keyboard.settings.ios.LocalIosColors
import kotlinx.coroutines.flow.collectLatest

@Composable
fun LiquidToggle(
    selected: () -> Boolean,
    onSelect: (Boolean) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    level: GlassLevel = IosGlass.levelFor(Build.VERSION.SDK_INT)
) {
    val colors = LocalIosColors.current
    val accentColor = colors.switchOn
    val trackColor = colors.switchOff
    // the gesture callbacks are created once; let them see the current lambdas
    val currentSelected by rememberUpdatedState(selected)
    val currentOnSelect by rememberUpdatedState(onSelect)

    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val dragWidth = with(density) { 20f.dp.toPx() }
    val animationScope = rememberCoroutineScope()
    var didDrag by remember { mutableStateOf(false) }
    var fraction by remember { mutableFloatStateOf(if (selected()) 1f else 0f) }
    val dampedDragAnimation = remember(animationScope) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = fraction,
            valueRange = 0f..1f,
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 1.5f,
            onDragStarted = {},
            onDragStopped = {
                if (didDrag) {
                    fraction = if (fraction >= 0.5f) 1f else 0f
                    currentOnSelect(fraction == 1f)
                    didDrag = false
                } else {
                    fraction = if (currentSelected()) 0f else 1f
                    currentOnSelect(fraction == 1f)
                }
            },
            onDrag = { _, dragAmount ->
                if (!didDrag) {
                    didDrag = dragAmount.x != 0f
                }
                val delta = dragAmount.x / dragWidth
                fraction =
                    if (isLtr) (fraction + delta).fastCoerceIn(0f, 1f)
                    else (fraction - delta).fastCoerceIn(0f, 1f)
            },
            onDragCancelled = {
                didDrag = false
                fraction = if (currentSelected()) 1f else 0f
            }
        )
    }
    LaunchedEffect(dampedDragAnimation) {
        snapshotFlow { fraction }
            .collectLatest { fraction ->
                dampedDragAnimation.updateValue(fraction)
            }
    }
    LaunchedEffect(selected) {
        snapshotFlow { selected() }
            .collectLatest { isSelected ->
                val target = if (isSelected) 1f else 0f
                if (target != fraction) {
                    fraction = target
                    dampedDragAnimation.animateToValue(target)
                }
            }
    }

    val trackBackdrop = rememberLayerBackdrop()
    val glass by remember { derivedStateOf { level != GlassLevel.FILL && dampedDragAnimation.pressProgress > 0f } }

    Box(
        modifier.then(dampedDragAnimation.modifier),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            Modifier
                .then(if (glass) Modifier.layerBackdrop(trackBackdrop) else Modifier)
                .clip(Capsule())
                .drawBehind {
                    val fraction = dampedDragAnimation.value
                    drawRect(lerp(trackColor, accentColor, fraction))
                }
                .size(64f.dp, 28f.dp)
        )

        Box(
            Modifier
                .graphicsLayer {
                    val fraction = dampedDragAnimation.value
                    val padding = 2f.dp.toPx()
                    translationX =
                        if (isLtr) lerp(padding, padding + dragWidth, fraction)
                        else lerp(-padding, -(padding + dragWidth), fraction)
                }
                .thumbSurface(
                    level = level,
                    glass = glass,
                    pressProgress = { dampedDragAnimation.pressProgress },
                    layerBlock = {
                        scaleX = dampedDragAnimation.scaleX
                        scaleY = dampedDragAnimation.scaleY
                        val velocity = dampedDragAnimation.velocity / 50f
                        scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                        scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                    },
                    backdrop = rememberCombinedBackdrop(
                        backdrop,
                        rememberBackdrop(trackBackdrop) { drawBackdrop ->
                            val progress = dampedDragAnimation.pressProgress
                            val scaleX = lerp(2f / 3f, 0.75f, progress)
                            val scaleY = lerp(0f, 0.75f, progress)
                            scale(scaleX, scaleY) {
                                drawBackdrop()
                            }
                        }
                    )
                )
                .size(40f.dp, 24f.dp)
        )
    }
}

private fun Modifier.thumbSurface(
    level: GlassLevel,
    glass: Boolean,
    pressProgress: () -> Float,
    layerBlock: GraphicsLayerScope.() -> Unit,
    backdrop: Backdrop
): Modifier = when {
    level == GlassLevel.FILL -> this
        .graphicsLayer {
            layerBlock()
            shadowElevation = 2f.dp.toPx()
            shape = Capsule()
            clip = true
        }
        .background(Color.White)
    !glass -> this.restingThumb(layerBlock)
    level == GlassLevel.LENS -> this
        .iosBackdropRefresh()
        .drawBackdrop(
            backdrop = backdrop,
            shape = { Capsule() },
            effects = {
                val progress = pressProgress()
                blur(8f.dp.toPx() * (1f - progress))
                lens(
                    5f.dp.toPx() * progress,
                    10f.dp.toPx() * progress,
                    chromaticAberration = true
                )
            },
            highlight = {
                val progress = pressProgress()
                Highlight.Ambient.copy(
                    width = Highlight.Ambient.width / 1.5f,
                    blurRadius = Highlight.Ambient.blurRadius / 1.5f,
                    alpha = progress
                )
            },
            shadow = {
                Shadow(
                    radius = 4f.dp,
                    color = Color.Black.copy(alpha = 0.05f)
                )
            },
            innerShadow = {
                val progress = pressProgress()
                InnerShadow(
                    radius = 4f.dp * progress,
                    alpha = progress
                )
            },
            layerBlock = layerBlock,
            onDrawSurface = {
                val progress = pressProgress()
                drawRect(Color.White.copy(alpha = 1f - progress))
            }
        )
    else -> this
        .iosBackdropRefresh()
        .drawBackdrop(
            backdrop = backdrop,
            shape = { Capsule() },
            effects = {
                blur(8f.dp.toPx())
            },
            shadow = {
                Shadow(
                    radius = 4f.dp,
                    color = Color.Black.copy(alpha = 0.05f)
                )
            },
            layerBlock = layerBlock,
            onDrawSurface = {
                val progress = pressProgress()
                drawRect(Color.White.copy(alpha = 1f - 0.4f * progress))
            }
        )
}

/**
 * The LENS / BLUR thumb at rest, as the glass thumb draws it there: an opaque white capsule over Kyant's
 * Shadow(radius = 4.dp, color = 5% black), a blurred copy of the shape moved down by radius / 6.
 */
private fun Modifier.restingThumb(layerBlock: GraphicsLayerScope.() -> Unit): Modifier = this
    .graphicsLayer(layerBlock)
    .drawWithCache {
        val radius = 4f.dp.toPx()
        val shadow = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.Black.copy(alpha = 0.05f).toArgb()
            maskFilter = BlurMaskFilter(radius, BlurMaskFilter.Blur.NORMAL)
        }
        val corner = size.height / 2
        onDrawBehind {
            drawIntoCanvas {
                it.nativeCanvas.drawRoundRect(0f, radius / 6, size.width, size.height + radius / 6, corner, corner, shadow)
            }
            drawRoundRect(Color.White, cornerRadius = CornerRadius(corner))
        }
    }
