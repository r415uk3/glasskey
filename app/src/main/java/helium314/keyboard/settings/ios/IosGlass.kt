// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.ios

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight

// The only file that touches io.github.kyant0:backdrop; everything else goes through these wrappers.

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

/** Records this node's content so that [iosGlass] elements can refract / blur it. */
fun Modifier.iosBackdropSource(backdrop: IosBackdrop): Modifier = layerBackdrop(backdrop)

@Composable
fun Modifier.iosGlass(
    backdrop: IosBackdrop,
    shape: Shape,
    level: GlassLevel = IosGlass.levelFor(Build.VERSION.SDK_INT),
): Modifier {
    val fill = LocalIosColors.current.groupFill
    val dark = LocalIosDark.current
    return when (level) {
        GlassLevel.LENS -> drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                vibrancy()
                blur(2.dp.toPx())
                // the library's lens supports rounded corner shapes only
                if (shape is CornerBasedShape) lens(12.dp.toPx(), 24.dp.toPx())
            },
            highlight = { Highlight.Default },
            onDrawSurface = { drawRect(fill.copy(alpha = fill.alpha / 2)) },
        )
        GlassLevel.BLUR -> drawBackdrop(
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
