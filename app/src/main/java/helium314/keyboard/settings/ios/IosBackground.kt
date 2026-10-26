// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.ios

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

private val blobs = listOf(
    Triple(Color(0xFF1F4FBF), Offset(0.15f, 0.20f), 0.40f),
    Triple(Color(0xFF7B2FF7), Offset(0.85f, 0.45f), 0.38f),
    Triple(Color(0xFF0F8F7F), Offset(0.30f, 0.85f), 0.40f),
)

/** Soft colour blobs behind the settings glass, so the glass has something to refract. */
@Composable
fun IosBackground(dark: Boolean, modifier: Modifier = Modifier) {
    val base = if (dark) Color(0xFF05060A) else Color(0xFFF2F2F7)
    val alpha = if (dark) 1f else 0.25f
    Canvas(modifier) {
        drawRect(base)
        blobs.forEach { (color, center, radius) -> drawBlob(color.copy(alpha = alpha), center, radius) }
    }
}

// radial gradients fading to transparent read as blurred blobs and need no RenderEffect (works below Android 12)
private fun DrawScope.drawBlob(color: Color, center: Offset, radius: Float) {
    val c = Offset(size.width * center.x, size.height * center.y)
    val r = size.maxDimension * radius
    drawCircle(
        brush = Brush.radialGradient(
            0f to color, 0.45f to color.copy(alpha = color.alpha * 0.5f), 1f to Color.Transparent,
            center = c, radius = r
        ),
        radius = r,
        center = c,
    )
}
