// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.ios

// ---------------------------------------------------------------------------------------------------------------
// TEMPORARY device diagnostics for the settings push / pop transitions (plan 5 task 9). Debug builds only; logs
// with tag "GlasskeyNav" (adb logcat -s GlasskeyNav) and, because the vivo test phone drops app logcat output, also
// to files/glasskey-nav.log (adb shell run-as io.github.r415uk3.glasskey.debug cat files/glasskey-nav.log; written
// when a navigation starts and when it has settled). Remove this file and its call sites once the transitions are
// confirmed: IosNavDiagnosticsEffect, Modifier.iosNavDiagnostics and the clock's onSlideStart / onGateEvaluate /
// onPagePlaced logs in IosSettingsNavHost; IosNavDiagnostics.pageOffset in IosNavMotion; the TEMPORARY onEvaluate,
// onGateEvaluate and onPagePlaced hooks in IosNavMotion.
// ---------------------------------------------------------------------------------------------------------------

import android.animation.ValueAnimator
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import helium314.keyboard.latin.BuildConfig
import helium314.keyboard.latin.settings.getTransitionAnimationScale
import kotlinx.coroutines.flow.collectLatest
import kotlin.coroutines.coroutineContext

internal object IosNavDiagnostics {
    const val TAG = "GlasskeyNav"
    val enabled = BuildConfig.DEBUG

    /** Uptime of the latest navigation, so per-page logs can say how long after it they happened. */
    @Volatile var navigatedAt = 0L

    /** Lines also go to files/glasskey-nav.log (adb shell run-as <pkg> cat files/glasskey-nav.log): some ROMs drop app logcat. */
    @Volatile var file: java.io.File? = null
    private val pending = StringBuilder()
    var gateLines = 0

    fun log(message: String) {
        if (!enabled) return
        Log.i(TAG, message)
        synchronized(pending) { pending.append(SystemClock.uptimeMillis()).append(' ').append(message).append('\n') }
    }

    fun flush() {
        val f = file ?: return
        val text = synchronized(pending) { pending.toString().also { pending.setLength(0) } }
        if (text.isNotEmpty()) runCatching { f.appendText(text) }
    }

    fun gateEvaluated(playTimeNanos: Long, localNanos: Long, open: Boolean) {
        if (!enabled || gateLines++ > 400) return
        log("+${sinceNav()}   gate play=${playTimeNanos / 1_000_000}ms local=${localNanos / 1_000_000}ms open=$open")
    }

    fun sinceNav() = if (navigatedAt == 0L) "-" else "${SystemClock.uptimeMillis() - navigatedAt}ms"

    /** From the page overlay: the page's leading-edge offset, as a fraction of the width, whenever it changes. */
    fun pageOffset(label: String, fraction: Float) {
        if (enabled) log("+${sinceNav()} page $label offset ${String.format(java.util.Locale.US, "%.3f", fraction)}")
    }

    fun firstCompose(label: String) = log("+${sinceNav()} page $label first composed")
    fun firstDraw(label: String) = log("+${sinceNav()} page $label first drawn")
}

/** Logs the animation scales once, then each frame of every navigation until it has settled (at most 1.5 s). */
@Composable
internal fun IosNavDiagnosticsEffect(navController: NavHostController, animationScale: Float, durationMillis: Int) {
    if (!IosNavDiagnostics.enabled) return
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        IosNavDiagnostics.file = java.io.File(context.filesDir, "glasskey-nav.log")
        val motion = coroutineContext[MotionDurationScale]?.scaleFactor
        val enabled = if (Build.VERSION.SDK_INT >= 26) ValueAnimator.areAnimatorsEnabled().toString() else "n/a"
        val animatorScale = if (Build.VERSION.SDK_INT >= 33) ValueAnimator.getDurationScale().toString() else "n/a"
        IosNavDiagnostics.log(
            "start: MotionDurationScale=$motion areAnimatorsEnabled=$enabled ValueAnimator.durationScale=$animatorScale " +
                "transitionScale(setting)=${getTransitionAnimationScale(context)} animationScale(used)=$animationScale " +
                "duration=${durationMillis}ms sdk=${Build.VERSION.SDK_INT}"
        )
    }
    LaunchedEffect(navController) {
        var previous: String? = null
        navController.currentBackStackEntryFlow.collectLatest { entry ->
            val route = entry.destination.route
            if (previous == null) { previous = route; return@collectLatest } // the start destination
            IosNavDiagnostics.flush()
            IosNavDiagnostics.navigatedAt = SystemClock.uptimeMillis()
            IosNavDiagnostics.gateLines = 0
            IosNavDiagnostics.log("navigate $previous -> $route, back stack ${navController.currentBackStack.value.size}")
            previous = route
            var start = -1L
            var settledFrames = 0
            while (settledFrames < 3) {
                val ms = withFrameNanos { t -> if (start < 0) start = t; (t - start) / 1_000_000 }
                val visible = navController.visibleEntries.value
                IosNavDiagnostics.log("  frame +${ms}ms visible=${visible.size} ${visible.map { it.destination.route }}")
                if (visible.size <= 1) settledFrames++ else settledFrames = 0
                if (ms > 1500) { IosNavDiagnostics.log("  still not settled after 1.5 s"); break }
            }
            IosNavDiagnostics.flush()
        }
    }
}

/** Logs when a page is first composed and first drawn. */
@Composable
internal fun Modifier.iosNavDiagnostics(label: String): Modifier {
    if (!IosNavDiagnostics.enabled) return this
    val drawn = remember { booleanArrayOf(false) }
    val composed = remember { booleanArrayOf(false) }
    SideEffect { if (!composed[0]) { composed[0] = true; IosNavDiagnostics.firstCompose(label) } }
    return drawWithContent {
        if (!drawn[0]) { drawn[0] = true; IosNavDiagnostics.firstDraw(label) }
        drawContent()
    }
}
