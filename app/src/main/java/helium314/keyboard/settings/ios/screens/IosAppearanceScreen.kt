// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.ios.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.edit
import helium314.keyboard.keyboard.KeyboardSwitcher
import helium314.keyboard.keyboard.KeyboardTypeface
import helium314.keyboard.latin.R
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.KeyboardFont
import helium314.keyboard.latin.utils.prefs
import helium314.keyboard.settings.ios.IosCheckRow
import helium314.keyboard.settings.ios.IosScreen
import helium314.keyboard.settings.ios.IosSwitchRow
import helium314.keyboard.settings.ios.IosThemeChoice
import helium314.keyboard.settings.ios.iosGroup
import helium314.keyboard.settings.ios.rememberPrefsVersion

private val themeTitles = mapOf(
    IosThemeChoice.SYSTEM to R.string.ios_theme_system,
    IosThemeChoice.LIGHT to R.string.ios_theme_light,
    IosThemeChoice.DARK to R.string.ios_theme_dark,
)

/** Rebuilds the keyboard with the changed look the next time it is shown. */
fun iosReloadKeyboard() = KeyboardSwitcher.getInstance().setThemeNeedsReload()

/** Тема, Шрифт, Ряд цифр and Размытие фона. */
@Composable
fun IosAppearanceScreen(onBack: () -> Unit) {
    val prefs = LocalContext.current.prefs()
    val version = rememberPrefsVersion()
    val theme = remember(version) { IosThemeChoice.read(prefs) }
    val font = remember(version) { KeyboardFont.fromPref(prefs.getString(Settings.PREF_KEYBOARD_FONT, Defaults.PREF_KEYBOARD_FONT)) }
    val themeHeader = stringResource(R.string.ios_theme).uppercase()
    val fontHeader = stringResource(R.string.ios_font).uppercase()
    val blurFooter = stringResource(R.string.ios_blur_footer)

    IosScreen(stringResource(R.string.ios_appearance), onBack) {
        iosGroup(header = themeHeader, rows = IosThemeChoice.entries.map { choice ->
            { ->
                IosCheckRow(stringResource(themeTitles.getValue(choice)), theme == choice) {
                    IosThemeChoice.write(prefs, choice)
                    iosReloadKeyboard()
                }
            }
        })
        iosGroup(header = fontHeader, rows = KeyboardFont.entries.map { entry ->
            { ->
                IosCheckRow(stringResource(entry.title), font == entry) {
                    prefs.edit { putString(Settings.PREF_KEYBOARD_FONT, entry.prefValue) }
                    KeyboardTypeface.clearCache()
                    iosReloadKeyboard()
                }
            }
        })
        iosGroup(rows = listOf {
            IosSwitchRow(stringResource(R.string.ios_number_row), Settings.PREF_SHOW_NUMBER_ROW, Defaults.PREF_SHOW_NUMBER_ROW) { iosReloadKeyboard() }
        })
        iosGroup(footer = blurFooter, rows = listOf {
            IosSwitchRow(stringResource(R.string.ios_blur), Settings.PREF_IOS_BLUR, Defaults.PREF_IOS_BLUR) { iosReloadKeyboard() }
        })
    }
}
