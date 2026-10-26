// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.ios

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import helium314.keyboard.latin.R

const val IosLargeTitleTag = "ios_large_title"
const val IosBackButtonTag = "ios_back_button"

/** What glass panels drawn here refract; provided by [IosScreen]. */
val LocalIosBackdrop = staticCompositionLocalOf<IosBackdrop?> { null }

private val barHeight = 56.dp

/** Glass over [LocalIosBackdrop], or a plain translucent fill outside an [IosScreen]. */
@Composable
fun Modifier.iosPanel(shape: Shape, level: GlassLevel = IosGlass.levelFor(Build.VERSION.SDK_INT)): Modifier {
    val fallback = rememberIosBackdrop()
    val backdrop = LocalIosBackdrop.current
    return if (backdrop != null) iosGlass(backdrop, shape, level) else iosGlass(fallback, shape, GlassLevel.FILL)
}

/**
 * Settings page: blob background, large title that collapses into a glass top bar, optional glass back button,
 * and an optional floating [bottomBar] (e.g. [IosSearchField]).
 */
@Composable
fun IosScreen(
    title: String,
    onBack: (() -> Unit)?,
    bottomBar: (@Composable () -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    val colors = LocalIosColors.current
    val dark = LocalIosDark.current
    // groups refract only the background; chrome (top bar, back button, bottom bar) refracts the scrolling list too
    val backgroundBackdrop = rememberIosBackdrop()
    val screenBackdrop = rememberIosBackdrop()
    val listState = rememberLazyListState()
    var titleHeightPx by remember { mutableIntStateOf(0) }
    val collapse by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else IosLargeTitle.collapse(listState.firstVisibleItemScrollOffset, titleHeightPx)
        }
    }
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(Modifier.fillMaxSize().background(colors.background)) {
        Box(Modifier.fillMaxSize().iosBackdropSource(screenBackdrop)) {
            IosBackground(dark, Modifier.fillMaxSize().iosBackdropSource(backgroundBackdrop))
            CompositionLocalProvider(LocalIosBackdrop provides backgroundBackdrop) {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(
                        top = statusTop + barHeight,
                        bottom = navBottom + if (bottomBar != null) 90.dp else 24.dp
                    ),
                ) {
                    item {
                        BasicText(
                            title,
                            Modifier
                                .testTag(IosLargeTitleTag)
                                .semantics { heading() }
                                .onSizeChanged { titleHeightPx = it.height }
                                .padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
                            style = TextStyle(color = colors.text, fontSize = 32.sp, fontWeight = FontWeight.Bold),
                        )
                    }
                    content()
                }
            }
        }

        Box(Modifier.fillMaxWidth().height(statusTop + barHeight)) {
            Box(Modifier.matchParentSize().graphicsLayer { alpha = collapse }.iosGlass(screenBackdrop, RectangleShape))
            BasicText(
                title,
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 76.dp, end = 76.dp, bottom = 17.dp)
                    .graphicsLayer { alpha = collapse },
                style = TextStyle(color = colors.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (onBack != null)
                BackButton(onBack, screenBackdrop, Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 6.dp))
        }

        if (bottomBar != null)
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            ) {
                CompositionLocalProvider(LocalIosBackdrop provides screenBackdrop, content = bottomBar)
            }
    }
}

@Composable
private fun BackButton(onBack: () -> Unit, backdrop: IosBackdrop, modifier: Modifier) {
    val color = LocalIosColors.current.text
    val description = stringResource(R.string.ios_back)
    val circle = RoundedCornerShape(percent = 50)
    Box(
        modifier
            .testTag(IosBackButtonTag)
            .size(44.dp)
            .iosGlass(backdrop, circle)
            .clickable(remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onBack)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(width = 11.dp, height = 19.dp)) {
            val path = Path().apply {
                moveTo(size.width * 0.85f, size.height * 0.05f)
                lineTo(size.width * 0.15f, size.height * 0.5f)
                lineTo(size.width * 0.85f, size.height * 0.95f)
            }
            drawPath(path, color, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}
