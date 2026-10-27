// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.ios

import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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

object IosRoute {
    const val Main = "ios_main"
    const val Keyboards = "ios_keyboards"
    const val AddKeyboard = "ios_add_keyboard"
    const val Layout = "ios_layout/" // + "{subtype}" (SettingsSubtype pref string)
    const val Dictionaries = "ios_dictionaries"
    const val TextReplacement = "ios_text_replacement"
    const val Appearance = "ios_appearance"
    const val About = "ios_about"
    const val Licenses = "ios_licenses"
}

private class HostCoordinates { var value: LayoutCoordinates? = null }

/**
 * The iOS settings pages with iOS push / pop: the new page slides in over the whole width from the trailing edge
 * while the old one moves [IosNavMotion.ParallaxFraction] toward the leading edge and dims. All pages are drawn over
 * one still background ([LocalIosWallpaper]). A predictive back gesture drives the pop.
 */
@Composable
fun IosSettingsNavHost(
    onClickBack: () -> Unit,
    needsEnabling: Boolean,
    onEnable: () -> Unit,
    animationScale: Float = getTransitionAnimationScale(LocalContext.current),
    navController: NavHostController = rememberNavController(),
) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val dir = if (rtl) -1 else 1
    val dark = LocalIosDark.current
    val duration = IosNavMotion.durationMillis(animationScale)
    val wallpaper = rememberIosBackdrop()
    val host = remember { HostCoordinates() }
    val hostCoordinates = remember { { host.value } }

    fun goBack() {
        if (!navController.popBackStack()) onClickBack()
    }
    val navigate: (String) -> Unit = { navController.navigate(it) }

    // while a predictive back gesture runs, the page follows the finger linearly; the back stack is popped only
    // when the gesture completes, so the target page is not yet the current one
    fun AnimatedContentTransitionScope<NavBackStackEntry>.spec(): FiniteAnimationSpec<IntOffset> {
        val interactive = targetState != navController.currentBackStackEntry
        return tween(duration, easing = if (interactive) LinearEasing else IosNavMotion.Easing)
    }
    val parallax = IosNavMotion.ParallaxFraction

    Box(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned { host.value = it }
    ) {
        IosNavDiagnosticsEffect(navController, animationScale, duration) // TEMPORARY, see IosNavDiagnostics
        IosBackground(dark, Modifier.fillMaxSize().iosBackdropSource(wallpaper))
        CompositionLocalProvider(LocalIosWallpaper provides wallpaper) {
            NavHost(
                navController = navController,
                startDestination = IosRoute.Main,
                enterTransition = {
                    if (duration == 0) EnterTransition.None
                    else slideInHorizontally(spec()) { +it * dir }
                },
                exitTransition = {
                    if (duration == 0) ExitTransition.None
                    else slideOutHorizontally(spec()) { (-it * dir * parallax).toInt() }
                },
                popEnterTransition = {
                    if (duration == 0) EnterTransition.None
                    else slideInHorizontally(spec()) { (-it * dir * parallax).toInt() }
                },
                popExitTransition = {
                    if (duration == 0) ExitTransition.None
                    else slideOutHorizontally(spec()) { +it * dir }
                },
            ) {
                fun NavGraphBuilder.page(route: String, content: @Composable (NavBackStackEntry) -> Unit) =
                    composable(route) { entry ->
                        Box(
                            Modifier
                                .fillMaxSize()
                                .iosNavDiagnostics(route) // TEMPORARY, see IosNavDiagnostics
                                .iosNavOverlay(hostCoordinates, dark, rtl, route)
                        ) { content(entry) }
                    }

                page(IosRoute.Main) { IosMainScreen(needsEnabling, onEnable, navigate) }
                page(IosRoute.Keyboards) { IosKeyboardsScreen(::goBack, navigate) }
                page(IosRoute.AddKeyboard) { IosAddKeyboardScreen(::goBack, navigate) }
                page(IosRoute.Layout + "{subtype}") {
                    IosLayoutScreen(
                        Uri.decode(it.arguments?.getString("subtype")!!),
                        ::goBack,
                        onAdded = { navController.popBackStack(IosRoute.Keyboards, inclusive = false) },
                    )
                }
                page(IosRoute.TextReplacement) { IosTextReplacementScreen(::goBack) }
                page(IosRoute.Appearance) { IosAppearanceScreen(::goBack) }
                page(IosRoute.About) { IosAboutScreen(::goBack, navigate) }
                page(IosRoute.Licenses) { IosLicensesScreen(::goBack) }
                page(IosRoute.Dictionaries) { IosDictionariesScreen(::goBack) }
            }
        }
    }
}
