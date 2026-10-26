// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.ios

import android.content.SharedPreferences
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import helium314.keyboard.latin.utils.prefs

const val IosRowChevronTag = "ios_row_chevron"

val IosGroupShape = RoundedCornerShape(26.dp)
private val sideInset = 16.dp

/** Coloured rounded square in front of a row title, with either a vector or a short text glyph. */
data class IosRowIcon(val background: Color, val vector: ImageVector? = null, val text: String? = null)

/** One glass panel with optional grey header above and footer below; separators are drawn between [rows]. */
fun LazyListScope.iosGroup(header: String? = null, footer: String? = null, rows: List<@Composable () -> Unit>) {
    item {
        IosGroupBox(header, footer, panel = rows.isNotEmpty()) {
            rows.forEachIndexed { i, row ->
                if (i > 0) IosSeparator()
                row()
            }
        }
    }
}

/** Layout of [iosGroup] for callers that lay out the rows themselves (e.g. a reorderable column). */
@Composable
fun IosGroupBox(header: String?, footer: String?, panel: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalIosColors.current
    Column(Modifier.fillMaxWidth().padding(bottom = 22.dp)) {
        if (header != null)
            BasicText(
                header,
                Modifier.padding(start = sideInset * 2, end = sideInset * 2, bottom = 7.dp),
                style = TextStyle(color = colors.secondaryText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            )
        if (panel)
            Column(
                Modifier.padding(horizontal = sideInset).fillMaxWidth()
                    .iosPanel(IosGroupShape)
                    .clip(IosGroupShape),
                content = content,
            )
        if (footer != null)
            BasicText(
                footer,
                Modifier.padding(start = sideInset * 2, end = sideInset * 2, top = 8.dp),
                style = TextStyle(color = colors.secondaryText, fontSize = 13.sp, lineHeight = 17.sp)
            )
    }
}

@Composable
fun IosSeparator() {
    Box(Modifier.padding(start = sideInset).fillMaxWidth().height(0.5.dp).background(LocalIosColors.current.separator))
}

/** [chevron] defaults to shown when the row is clickable. */
@Composable
fun IosRow(
    title: String,
    value: String? = null,
    icon: IosRowIcon? = null,
    chevron: Boolean? = null,
    titleColor: Color? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    subtitle: String? = null,
) = IosRowImpl(title, value, icon, chevron ?: (onClick != null), titleColor, onClick, trailing, modifier, leading, subtitle)

@Composable
private fun IosRowImpl(
    title: String,
    value: String?,
    icon: IosRowIcon?,
    chevron: Boolean,
    titleColor: Color?,
    onClick: (() -> Unit)?,
    trailing: (@Composable () -> Unit)?,
    modifier: Modifier,
    leading: (@Composable () -> Unit)?,
    subtitle: String?,
) {
    val colors = LocalIosColors.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(interaction, indication = null, onClick = onClick) else Modifier)
            .background(if (pressed) colors.separator else Color.Transparent)
            .heightIn(min = 46.dp)
            .padding(horizontal = sideInset),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(4.dp))
        }
        if (icon != null) {
            RowIcon(icon)
            Spacer(Modifier.width(6.dp)) // 12dp to the title together with the arrangement spacing
        }
        Column(Modifier.weight(1f).padding(vertical = if (subtitle == null) 11.dp else 7.dp)) {
            BasicText(title, style = TextStyle(color = titleColor ?: colors.text, fontSize = 16.sp))
            if (subtitle != null)
                BasicText(subtitle, style = TextStyle(color = colors.secondaryText, fontSize = 13.sp))
        }
        if (value != null)
            BasicText(
                value,
                style = TextStyle(color = colors.secondaryText, fontSize = 16.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        trailing?.invoke()
        if (chevron) Chevron(colors.secondaryText)
    }
}

@Composable
private fun RowIcon(icon: IosRowIcon) {
    Box(
        Modifier.size(30.dp).background(icon.background, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (icon.vector != null)
            Image(icon.vector, null, Modifier.size(19.dp), colorFilter = ColorFilter.tint(Color.White))
        else if (icon.text != null)
            BasicText(icon.text, style = TextStyle(color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold))
    }
}

@Composable
private fun Chevron(color: Color) {
    Canvas(Modifier.testTag(IosRowChevronTag).size(width = 8.dp, height = 14.dp)) {
        val path = Path().apply {
            moveTo(size.width * 0.2f, size.height * 0.1f)
            lineTo(size.width * 0.8f, size.height * 0.5f)
            lineTo(size.width * 0.2f, size.height * 0.9f)
        }
        drawPath(path, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** Increases whenever any pref changes, so values derived from prefs can be re-read with `remember(version)`. */
@Composable
fun rememberPrefsVersion(): Int {
    val prefs = LocalContext.current.prefs()
    var version by remember { mutableIntStateOf(0) }
    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> version++ }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return version
}

/** Row with a switch bound to a boolean pref; tapping anywhere on the row toggles it. */
@Composable
fun IosSwitchRow(title: String, prefKey: String, default: Boolean, onChange: ((Boolean) -> Unit)? = null) {
    val prefs = LocalContext.current.prefs()
    var checked by remember(prefKey) { mutableStateOf(prefs.getBoolean(prefKey, default)) }
    DisposableEffect(prefs, prefKey) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
            if (key == prefKey) checked = p.getBoolean(prefKey, default)
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    val set: (Boolean) -> Unit = { value ->
        checked = value
        prefs.edit().putBoolean(prefKey, value).apply()
        onChange?.invoke(value)
    }
    IosRow(
        title,
        chevron = false,
        onClick = { set(!checked) },
        trailing = { IosSwitch(checked, set, Modifier.clearAndSetSemantics { testTag = IosSwitchTag }) },
        modifier = Modifier.semantics {
            role = Role.Switch
            toggleableState = ToggleableState(checked)
        },
    )
}

/** Selectable row with a tint check mark on the right when [checked]. */
@Composable
fun IosCheckRow(title: String, checked: Boolean, onClick: () -> Unit) {
    val tint = LocalIosColors.current.tint
    IosRow(
        title,
        chevron = false,
        onClick = onClick,
        trailing = {
            Canvas(Modifier.size(width = 16.dp, height = 13.dp)) {
                if (!checked) return@Canvas
                val path = Path().apply {
                    moveTo(size.width * 0.08f, size.height * 0.55f)
                    lineTo(size.width * 0.38f, size.height * 0.88f)
                    lineTo(size.width * 0.92f, size.height * 0.1f)
                }
                drawPath(path, tint, style = Stroke(2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        },
        modifier = Modifier.semantics { selected = checked },
    )
}
