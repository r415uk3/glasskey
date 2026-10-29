// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.ios

import android.net.Uri
import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.LayoutDirection
import helium314.keyboard.latin.settings.getTransitionAnimationScale
import helium314.keyboard.settings.ios.screens.IosAboutScreen
import helium314.keyboard.settings.ios.screens.IosAddKeyboardScreen
import helium314.keyboard.settings.ios.screens.IosAppearanceScreen
import helium314.keyboard.settings.ios.screens.IosDictionariesScreen
import helium314.keyboard.settings.ios.screens.IosKeyboardsScreen
import helium314.keyboard.settings.ios.screens.IosLayoutScreen
import helium314.keyboard.settings.ios.screens.IosLicensesScreen
import helium314.keyboard.settings.ios.screens.IosMainScreen
import helium314.keyboard.settings.ios.screens.IosTextReplacementScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt

object IosRoute {
    const val Main = "ios_main"
    const val Keyboards = "ios_keyboards"
    const val AddKeyboard = "ios_add_keyboard"
    const val Layout = "ios_layout/" // + Uri.encode(SettingsSubtype pref string)
    const val Dictionaries = "ios_dictionaries"
    const val TextReplacement = "ios_text_replacement"
    const val Appearance = "ios_appearance"
    const val About = "ios_about"
    const val Licenses = "ios_licenses"
}

/** One page on the [IosNavStack]; [id] keys its composition and saved state. */
class IosNavEntry(val id: Long, val route: String) {
    internal var placed = false
    internal var onPlaced: (() -> Unit)? = null
}

/**
 * The iOS settings pages as a stack, like a UINavigationController: pages below the top one stay composed (hidden),
 * so going back shows them at once, as they were. [position] is where the top page is: 0 in place, 1 off screen at
 * the trailing edge; the page below sits [IosNavMotion.ParallaxFraction] toward the leading edge when it is 1.
 */
class IosNavStack internal constructor(initial: List<IosNavEntry>, private var nextId: Long) {
    val entries = mutableStateListOf<IosNavEntry>().apply { addAll(initial) }
    var position by mutableFloatStateOf(0f)
        internal set
    /** A push, pop or back gesture is running: the page below is shown too, and taps on the pages are ignored. */
    var transitioning by mutableStateOf(false)
        private set

    /**
     * Pages composed and laid out ahead of time, never shown, for routes the top page can push
     * ([IosNavMotion.prewarmRoutes]); a push takes one, so its first frame only draws. Not saved.
     */
    internal val spares = mutableStateListOf<IosNavEntry>()
    /** The pages were touched since the last check; spares are prepared only when the user is not touching. */
    internal var touched = false

    internal var durationMillis = IosNavMotion.DurationMillis
    internal lateinit var scope: CoroutineScope
    internal var onLeaveRoot: () -> Unit = {}
    private var job: Job? = null

    val currentRoute: String get() = entries.last().route

    private fun runTransition(block: suspend () -> Unit) {
        transitioning = true
        val previous = job
        job = scope.launch {
            previous?.cancel()
            block()
            transitioning = false
        }
    }

    private suspend fun slide(to: Float, millis: Int) {
        if (millis <= 0 || position == to) { position = to; return }
        animate(position, to, animationSpec = tween(millis, easing = IosNavMotion.Easing)) { v, _ -> position = v }
    }

    /** Slides [route] in over the current page. Ignored while another transition runs (as on iOS). */
    fun push(route: String) {
        if (transitioning) return
        // a page prepared ahead of time only has to be drawn; otherwise it is composed and laid out first
        val entry = spares.firstOrNull { it.route == route }?.also { spares.remove(it) } ?: IosNavEntry(nextId++, route)
        entries += entry
        if (durationMillis == 0) return
        position = 1f // the new page is composed, laid out and drawn off screen first; that frame can be slow
        runTransition {
            // the curve starts on the first frame after the new page is on screen, so none of it is lost to that frame
            if (!entry.placed) kotlinx.coroutines.suspendCancellableCoroutine { c ->
                entry.onPlaced = { entry.onPlaced = null; c.resumeWith(Result.success(Unit)) }
                c.invokeOnCancellation { entry.onPlaced = null }
            }
            slide(0f, durationMillis)
        }
    }

    /** Slides the top page out; on the first page, leaves the settings. Ignored while another transition runs. */
    fun pop() {
        if (transitioning) return
        if (entries.size < 2) { onLeaveRoot(); return }
        runTransition { finishPop(durationMillis) }
    }

    /** Pops to the topmost page with [route]: the pages in between go at once, the top one slides out. */
    fun popTo(route: String) {
        if (transitioning) return
        val target = entries.indexOfLast { it.route == route }
        if (target < 0 || target == entries.lastIndex) return
        while (entries.size - 2 > target) entries.removeAt(entries.size - 2)
        pop()
    }

    private suspend fun finishPop(millis: Int) {
        slide(1f, millis)
        entries.removeAt(entries.lastIndex)
        position = 0f
    }

    /**
     * The predictive back gesture: the top page follows the finger (from the edge it started at, 1:1, never back
     * behind where the page already is), slides out when the gesture completes and back in when it is cancelled.
     * It may start during a push; the page is then caught where it is.
     */
    internal suspend fun backGesture(progress: Flow<BackEventCompat>, width: () -> Float, rtl: Boolean) {
        job?.cancel()
        transitioning = true
        val base = position
        try {
            progress.collect { event ->
                position = maxOf(base, IosNavMotion.gestureFraction(event, width(), rtl)).coerceIn(0f, 1f)
            }
        } catch (e: CancellationException) {
            job = scope.launch {
                slide(0f, (durationMillis * position).roundToInt())
                transitioning = false
            }
            throw e
        }
        // the rest of the way at the normal speed, decelerating
        job = scope.launch {
            finishPop((durationMillis * (1f - position)).roundToInt())
            transitioning = false
        }
    }

    /** Prepares the next missing spare for the top page, if any; false when all are there. */
    internal fun addSpare(): Boolean {
        // spares of pages further down stay: going back there, they are ready at once
        val wanted = IosNavMotion.prewarmRoutes(currentRoute)
        val missing = wanted.firstOrNull { r -> spares.none { it.route == r } } ?: return false
        spares += IosNavEntry(nextId++, missing)
        return true
    }

    companion object {
        fun saver() = listSaver<IosNavStack, String>(
            save = { s -> listOf(s.nextId.toString()) + s.entries.map { "${it.id}|${it.route}" } },
            restore = { l ->
                IosNavStack(l.drop(1).map { val (id, route) = it.split('|', limit = 2); IosNavEntry(id.toLong(), route) }, l[0].toLong())
            },
        )
    }
}

@Composable
fun rememberIosNavStack(): IosNavStack =
    rememberSaveable(saver = IosNavStack.saver()) { IosNavStack(listOf(IosNavEntry(0, IosRoute.Main)), 1) }

private class HostCoordinates { var value: LayoutCoordinates? = null }

/**
 * The iOS settings pages with iOS push / pop: the new page slides in over the whole width from the trailing edge
 * while the old one moves [IosNavMotion.ParallaxFraction] toward the leading edge and dims. All pages are drawn over
 * one still background ([LocalIosWallpaper]). The predictive back gesture drags the top page with the finger.
 */
@Composable
fun IosSettingsNavHost(
    onClickBack: () -> Unit,
    needsEnabling: Boolean,
    onEnable: () -> Unit,
    animationScale: Float = getTransitionAnimationScale(LocalContext.current),
    stack: IosNavStack = rememberIosNavStack(),
) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val dark = LocalIosDark.current
    val wallpaper = rememberIosBackdrop()
    val host = remember { HostCoordinates() }
    val hostCoordinates = remember { { host.value } }
    val holder = rememberSaveableStateHolder()
    stack.scope = rememberCoroutineScope()
    stack.durationMillis = IosNavMotion.durationMillis(animationScale)
    stack.onLeaveRoot = onClickBack

    val goBack: () -> Unit = { stack.pop() }
    val navigate: (String) -> Unit = { stack.push(it) }

    // prepare the pages the top page can push while nothing happens (the composition takes a while on the main thread)
    LaunchedEffect(stack.currentRoute, stack.transitioning) {
        if (stack.transitioning) return@LaunchedEffect
        while (true) {
            delay(IosNavMotion.PrewarmIdleMillis)
            if (stack.touched) { stack.touched = false; continue }
            if (!stack.addSpare()) break
        }
    }
    PredictiveBackHandler(enabled = stack.entries.size > 1) { progress ->
        stack.backGesture(progress, { host.value?.size?.width?.toFloat() ?: 1f }, rtl)
    }

    Box(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned { host.value = it }
            .pointerInput(stack) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial)
                        stack.touched = true
                    }
                }
            }
    ) {
        IosBackground(dark, Modifier.fillMaxSize().iosBackdropSource(wallpaper))
        CompositionLocalProvider(LocalIosWallpaper provides wallpaper) {
            val top = stack.entries.lastIndex
            // spares first (hidden), so that a spare becoming the top page keeps its composition (moved by key)
            (stack.spares + stack.entries).forEach { entry ->
                key(entry.id) {
                    // a popped page's saved state goes with it
                    DisposableEffect(Unit) {
                        onDispose { if (entry !in stack.entries && entry !in stack.spares) holder.removeState(entry.id.toString()) }
                    }
                    val i = stack.entries.indexOf(entry)
                    val shown = i >= 0 && (i == top || (i == top - 1 && stack.transitioning))
                    Box(
                        Modifier
                            .fillMaxSize()
                            .then(if (shown) Modifier else Modifier.clearAndSetSemantics {})
                            .layout { measurable, constraints ->
                                val page = measurable.measure(constraints)
                                layout(page.width, page.height) {
                                    val x = when {
                                        // a spare waits just off screen where a push starts, drawn once already
                                        i < 0 -> page.width.toFloat()
                                        // pages further down stay composed and measured, but are not placed (not drawn)
                                        !shown -> return@layout
                                        i == top -> stack.position * page.width
                                        else -> -IosNavMotion.ParallaxFraction * (1f - stack.position) * page.width
                                    }
                                    page.placeRelative(x.roundToInt(), 0)
                                    if (!entry.placed) { entry.placed = true; entry.onPlaced?.invoke() }
                                }
                            }
                            // a spare casts no shadow onto the screen edge
                            .then(if (i < 0) Modifier else Modifier.iosNavOverlay(hostCoordinates, dark, rtl, entry.route))
                            .pointerInput(Unit) {} // a hit on this page does not fall through to the one below
                    ) {
                        holder.SaveableStateProvider(entry.id.toString()) { Page(entry.route, stack, needsEnabling, onEnable, goBack, navigate) }
                    }
                }
            }
            // no taps on the pages while they move (as on iOS)
            if (stack.transitioning)
                Box(Modifier.fillMaxSize().pointerInput(Unit) {
                    awaitPointerEventScope { while (true) awaitPointerEvent().changes.forEach { it.consume() } }
                })
        }
    }
}

@Composable
private fun Page(
    route: String,
    stack: IosNavStack,
    needsEnabling: Boolean,
    onEnable: () -> Unit,
    goBack: () -> Unit,
    navigate: (String) -> Unit,
) {
    when {
        route == IosRoute.Main -> IosMainScreen(needsEnabling, onEnable, navigate)
        route == IosRoute.Keyboards -> IosKeyboardsScreen(goBack, navigate)
        route == IosRoute.AddKeyboard -> IosAddKeyboardScreen(goBack, navigate)
        route.startsWith(IosRoute.Layout) -> IosLayoutScreen(
            Uri.decode(route.removePrefix(IosRoute.Layout)),
            goBack,
            onAdded = { stack.popTo(IosRoute.Keyboards) },
        )
        route == IosRoute.TextReplacement -> IosTextReplacementScreen(goBack)
        route == IosRoute.Appearance -> IosAppearanceScreen(goBack)
        route == IosRoute.About -> IosAboutScreen(goBack, navigate)
        route == IosRoute.Licenses -> IosLicensesScreen(goBack)
        route == IosRoute.Dictionaries -> IosDictionariesScreen(goBack)
    }
}
