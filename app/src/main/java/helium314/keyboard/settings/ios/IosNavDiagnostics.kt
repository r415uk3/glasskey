// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.ios

// ---------------------------------------------------------------------------------------------------------------
// TEMPORARY device diagnostics for the settings push / pop transitions (plan 5 task 9). Debug builds only; logs
// with tag "GlasskeyNav" (adb logcat -s GlasskeyNav). Remove this file and its three call sites
// (IosNavDiagnosticsEffect and Modifier.iosNavDiagnostics in IosSettingsNavHost, IosNavDiagnostics.pageOffset in
// IosNavMotion) once the cause is known.
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

    fun log(message: String) {
        if (enabled) Log.i(TAG, message)
    }

    private fun sinceNav() = if (navigatedAt == 0L) "-" else "${SystemClock.uptimeMillis() - navigatedAt}ms"

    /** From the page overlay: the page's leading-edge offset, as a fraction of the width, whenever it changes. */
    fun pageOffset(label: String, fraction: Float) {
        if (enabled) Log.i(TAG, "+${sinceNav()} page $label offset ${String.format(java.util.Locale.US, "%.3f", fraction)}")
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
            IosNavDiagnostics.navigatedAt = SystemClock.uptimeMillis()
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
