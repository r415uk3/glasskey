// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.ios

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import helium314.keyboard.latin.R
import helium314.keyboard.settings.ios.liquid.liquidButtonSurface
import helium314.keyboard.settings.ios.liquid.rememberInteractiveHighlight

const val IosSearchFieldTag = "ios_search_field"

private val fieldShape = RoundedCornerShape(24.dp)

/** Floating glass search pill, meant for the bottom bar of an [IosScreen]. */
@Composable
fun IosSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = stringResource(R.string.label_search_key),
) {
    val colors = LocalIosColors.current
    // LiquidButton's glass surface and press response; the text field keeps its own tap / selection handling
    val backdrop = LocalIosBackdrop.current
    val fallback = rememberIosBackdrop()
    val level = if (backdrop != null) IosGlass.levelFor(Build.VERSION.SDK_INT) else GlassLevel.FILL
    val highlight = rememberInteractiveHighlight(level)
    Row(
        modifier
            .testTag(IosSearchFieldTag)
            .fillMaxWidth()
            .height(48.dp)
            .liquidButtonSurface(
                backdrop ?: fallback,
                { fieldShape },
                level,
                highlight,
                surfaceColor = iosGlassSurface(level),
            )
            .then(highlight.modifier)
            .then(highlight.gestureModifier)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(17.dp)) {
            val stroke = 2.dp.toPx()
            val r = size.width * 0.34f
            val c = Offset(r + stroke / 2, r + stroke / 2)
            drawCircle(colors.secondaryText, r, c, style = Stroke(stroke))
            val d = r * 0.7071f
            drawLine(colors.secondaryText, Offset(c.x + d, c.y + d), Offset(size.width - stroke / 2, size.height - stroke / 2), stroke, StrokeCap.Round)
        }
        Spacer(Modifier.width(8.dp))
        BasicTextField(
            query,
            onQueryChange,
            Modifier.weight(1f),
            singleLine = true,
            textStyle = TextStyle(color = colors.text, fontSize = 17.sp),
            cursorBrush = SolidColor(colors.tint),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty())
                        BasicText(placeholder, style = TextStyle(color = colors.secondaryText, fontSize = 17.sp))
                    inner()
                }
            },
        )
        if (query.isNotEmpty())
            Box(
                Modifier
                    .padding(start = 8.dp)
                    .size(18.dp)
                    .clickable(remember { MutableInteractionSource() }, indication = null, role = Role.Button) { onQueryChange("") },
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(18.dp)) {
                    drawCircle(colors.secondaryText)
                    val a = size.width * 0.32f
                    val b = size.width * 0.68f
                    val w = 1.6.dp.toPx()
                    drawLine(colors.background, Offset(a, a), Offset(b, b), w, StrokeCap.Round)
                    drawLine(colors.background, Offset(b, a), Offset(a, b), w, StrokeCap.Round)
                }
            }
    }
}
