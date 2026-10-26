// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.ios

import android.net.Uri
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import helium314.keyboard.latin.R
import helium314.keyboard.latin.settings.getTransitionAnimationScale
import helium314.keyboard.settings.ios.screens.IosAddKeyboardScreen
import helium314.keyboard.settings.ios.screens.IosKeyboardsScreen
import helium314.keyboard.settings.ios.screens.IosLayoutScreen
import helium314.keyboard.settings.ios.screens.IosMainScreen
import helium314.keyboard.settings.screens.DictionaryScreen

object IosRoute {
    const val Main = "ios_main"
    const val Keyboards = "ios_keyboards"
    const val AddKeyboard = "ios_add_keyboard"
    const val Layout = "ios_layout/" // + "{subtype}" (SettingsSubtype pref string)
    const val Dictionaries = "ios_dictionaries"
    const val TextReplacement = "ios_text_replacement"
    const val Appearance = "ios_appearance"
    const val About = "ios_about"
}

@Composable
fun IosSettingsNavHost(onClickBack: () -> Unit, needsEnabling: Boolean, onEnable: () -> Unit) {
    val navController = rememberNavController()
    val dir = if (LocalLayoutDirection.current == LayoutDirection.Ltr) 1 else -1
    val duration = (300 * getTransitionAnimationScale(LocalContext.current)).toInt()
    val animation = tween<IntOffset>(durationMillis = duration)

    fun goBack() {
        if (!navController.popBackStack()) onClickBack()
    }
    val navigate: (String) -> Unit = { navController.navigate(it) }

    NavHost(
        navController = navController,
        startDestination = IosRoute.Main,
        enterTransition = { slideInHorizontally(initialOffsetX = { +it * dir }, animationSpec = animation) },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -it * dir / 3 }, animationSpec = animation) },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it * dir / 3 }, animationSpec = animation) },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { +it * dir }, animationSpec = animation) },
    ) {
        composable(IosRoute.Main) { IosMainScreen(needsEnabling, onEnable, navigate) }
        // placeholders, filled in by the following screens
        composable(IosRoute.Keyboards) { IosKeyboardsScreen(::goBack, navigate) }
        composable(IosRoute.AddKeyboard) { IosAddKeyboardScreen(::goBack, navigate) }
        composable(IosRoute.Layout + "{subtype}") {
            IosLayoutScreen(
                Uri.decode(it.arguments?.getString("subtype")!!),
                ::goBack,
                onAdded = { navController.popBackStack(IosRoute.Keyboards, inclusive = false) },
            )
        }
        composable(IosRoute.TextReplacement) { IosScreen(stringResource(R.string.ios_text_replacement), ::goBack) {} }
        composable(IosRoute.Appearance) { IosScreen(stringResource(R.string.ios_appearance), ::goBack) {} }
        composable(IosRoute.About) { IosScreen(stringResource(R.string.ios_about), ::goBack) {} }
        composable(IosRoute.Dictionaries) { DictionaryScreen(onClickBack = ::goBack) }
    }
}
