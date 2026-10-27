// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.ios

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import helium314.keyboard.latin.R
import helium314.keyboard.settings.ios.liquid.LiquidButton

const val IosLargeTitleTag = "ios_large_title"
const val IosBackButtonTag = "ios_back_button"

/** What glass panels drawn here refract; provided by [IosScreen]. */
val LocalIosBackdrop = staticCompositionLocalOf<IosBackdrop?> { null }

/**
 * The settings background shared by all pages, recorded once behind the navigation host so that it stays still
 * while pages slide over it; without it (a page shown on its own) each [IosScreen] draws its own.
 */
val LocalIosWallpaper = staticCompositionLocalOf<IosBackdrop?> { null }

private val barHeight = 56.dp
private val edgeOverhang = 12.dp

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
    action: (@Composable () -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    val colors = LocalIosColors.current
    val dark = LocalIosDark.current
    // groups refract only the background; chrome (top bar, back button, bottom bar) refracts the scrolling list too
    val wallpaper = LocalIosWallpaper.current
    val backgroundBackdrop = wallpaper ?: rememberIosBackdrop() // whether there is a wallpaper never changes
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

    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().iosBackdropSource(screenBackdrop)) {
            // opaque, so a page sliding in hides the one underneath; with a shared wallpaper it shows that one,
            // exactly where it is, so it does not move with the page
            // (the wallpaper is recorded behind the navigation host earlier in the same frame)
            if (wallpaper != null) Box(Modifier.fillMaxSize().iosWallpaper(wallpaper))
            else IosBackground(dark, Modifier.fillMaxSize().iosBackdropSource(backgroundBackdrop))
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
                                .graphicsLayer { alpha = IosLargeTitle.alpha(collapse) }
                                .padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
                            style = TextStyle(color = colors.text, fontSize = 32.sp, fontWeight = FontWeight.Bold),
                        )
                    }
                    content()
                }
            }
        }

        // the status-bar area belongs to the bar; its background reaches a little below the bar and fades out there
        Box(
            Modifier
                .fillMaxWidth()
                .height(statusTop + barHeight + edgeOverhang)
                .graphicsLayer { alpha = collapse }
                .iosScrollEdge(screenBackdrop, fade = edgeOverhang * 2)
        )
        Box(Modifier.fillMaxWidth().height(statusTop + barHeight)) {
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
            if (action != null)
                Box(Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 6.dp)) {
                    CompositionLocalProvider(LocalIosBackdrop provides screenBackdrop, content = action)
                }
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

/** Glass capsule with a tint text label for the top bar, e.g. «Изменить»: Kyant's LiquidButton. */
@Composable
fun IosBarButton(text: String, onClick: () -> Unit) {
    IosGlassButton(onClick) {
        BasicText(text, style = TextStyle(color = LocalIosColors.current.tint, fontSize = 17.sp, fontWeight = FontWeight.SemiBold))
    }
}

/**
 * Kyant's LiquidButton over [LocalIosBackdrop] (outside an [IosScreen]: a plain translucent fill), 44dp high;
 * [circle] makes it a 44dp circle for an icon.
 */
@Composable
fun IosGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    circle: Boolean = false,
    backdrop: IosBackdrop? = LocalIosBackdrop.current,
    content: @Composable RowScope.() -> Unit,
) {
    val fallback = rememberIosBackdrop()
    val level = if (backdrop != null) IosGlass.levelFor(Build.VERSION.SDK_INT) else GlassLevel.FILL
    LiquidButton(
        onClick,
        backdrop ?: fallback,
        if (circle) modifier.size(44.dp) else modifier,
        // the same fill as the other glass panels (iosGlass) keeps the label legible over busy content
        surfaceColor = iosGlassSurface(level),
        level = level,
        height = 44.dp,
        horizontalPadding = if (circle) 0.dp else 16.dp,
        content = content,
    )
}

@Composable
private fun BackButton(onBack: () -> Unit, backdrop: IosBackdrop, modifier: Modifier) {
    val color = LocalIosColors.current.text
    val description = stringResource(R.string.ios_back)
    IosGlassButton(
        onBack,
        modifier
            .testTag(IosBackButtonTag)
            .semantics { contentDescription = description },
        circle = true,
        backdrop = backdrop,
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
