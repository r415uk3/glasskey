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

// mockup v3: radial-gradient(circle at x y, color 0, transparent stop) on #05060a, blurred 40px
private val darkStops = listOf(0.40f, 0.38f, 0.40f)

/** Soft colour blobs behind the settings glass, so the glass has something to refract. */
@Composable
fun IosBackground(dark: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        if (dark) {
            drawRect(Color(0xFF05060A))
            blobs.forEachIndexed { i, (color, center, _) -> drawDarkBlob(color, center, darkStops[i]) }
        } else {
            drawRect(Color(0xFFF2F2F7))
            blobs.forEach { (color, center, radius) -> drawBlob(color.copy(alpha = 0.25f), center, radius) }
        }
    }
}

// CSS fades linearly from the full colour at the centre to transparent at the stop; the 40px blur lowers the peak
// and spreads the edge a little, approximated by a softer centre and a slightly larger radius
private fun DrawScope.drawDarkBlob(color: Color, center: Offset, stop: Float) {
    val c = Offset(size.width * center.x, size.height * center.y)
    val r = IosBackgroundBlobs.radius(center.x, center.y, size.width, size.height, stop) * 1.05f
    drawCircle(
        brush = Brush.radialGradient(
            0f to color.copy(alpha = 0.8f), 0.5f to color.copy(alpha = 0.35f), 1f to Color.Transparent,
            center = c, radius = r
        ),
        radius = r,
        center = c,
    )
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
