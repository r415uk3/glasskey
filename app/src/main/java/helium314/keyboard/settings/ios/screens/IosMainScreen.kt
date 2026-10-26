// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.ios.screens

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import helium314.keyboard.latin.R
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.KeyboardFont
import helium314.keyboard.latin.utils.SubtypeSettings
import helium314.keyboard.latin.utils.locale
import helium314.keyboard.latin.utils.prefs
import helium314.keyboard.settings.SettingsActivity
import helium314.keyboard.settings.ios.IOS_SEARCHABLE_KEYS
import helium314.keyboard.settings.ios.IosRoute
import helium314.keyboard.settings.ios.IosRow
import helium314.keyboard.settings.ios.IosRowIcon
import helium314.keyboard.settings.ios.IosScreen
import helium314.keyboard.settings.ios.IosSearchField
import helium314.keyboard.settings.ios.IosSwitchRow
import helium314.keyboard.settings.ios.LocalIosColors
import helium314.keyboard.settings.ios.iosGroup
import helium314.keyboard.settings.ios.rememberPrefsVersion

private class IosSwitchItem(val key: String, val title: Int, val default: Boolean, val reloadsKeyboard: Boolean = false)

private val allKeyboardsSwitches = listOf(
    IosSwitchItem(Settings.PREF_AUTO_CAP, R.string.ios_auto_cap, Defaults.PREF_AUTO_CAP),
    IosSwitchItem(Settings.PREF_AUTO_CORRECTION, R.string.ios_auto_correction, Defaults.PREF_AUTO_CORRECTION),
    IosSwitchItem(Settings.PREF_SHOW_SUGGESTIONS, R.string.ios_predictive, Defaults.PREF_SHOW_SUGGESTIONS),
    IosSwitchItem(Settings.PREF_POPUP_ON, R.string.ios_character_preview, Defaults.PREF_POPUP_ON),
    IosSwitchItem(Settings.PREF_KEY_USE_DOUBLE_SPACE_PERIOD, R.string.ios_period_shortcut, Defaults.PREF_KEY_USE_DOUBLE_SPACE_PERIOD),
)
private val feedbackSwitches = listOf(
    IosSwitchItem(Settings.PREF_SOUND_ON, R.string.ios_sound, Defaults.PREF_SOUND_ON),
    IosSwitchItem(Settings.PREF_VIBRATE_ON, R.string.ios_vibration, Defaults.PREF_VIBRATE_ON),
)
private val appearanceSwitches = listOf(
    IosSwitchItem(Settings.PREF_SHOW_NUMBER_ROW, R.string.ios_number_row, Defaults.PREF_SHOW_NUMBER_ROW, reloadsKeyboard = true),
    IosSwitchItem(Settings.PREF_IOS_BLUR, R.string.ios_blur, Defaults.PREF_IOS_BLUR, reloadsKeyboard = true),
)

/** A search hit: a switch, or a row leading to the screen that owns a non-boolean setting. */
private sealed interface IosSearchHit {
    data class Switch(val item: IosSwitchItem) : IosSearchHit
    data class Link(val title: Int, val route: String) : IosSearchHit
}

private val searchHits: List<Pair<Set<String>, IosSearchHit>> =
    (allKeyboardsSwitches + feedbackSwitches + appearanceSwitches).map { setOf(it.key) to IosSearchHit.Switch(it) } + listOf(
        setOf(Settings.PREF_KEYBOARD_FONT) to IosSearchHit.Link(R.string.ios_font, IosRoute.Appearance),
        setOf(Settings.PREF_THEME_DAY_NIGHT, Settings.PREF_THEME_COLORS) to IosSearchHit.Link(R.string.ios_theme, IosRoute.Appearance),
    )

private fun titleMatches(title: String, term: String): Boolean =
    title.lowercase().split(' ').any { it.trimStart('«', '“', '"').startsWith(term) }

/** Kept settings matching [query] by their HeliBoard title/description or by the iOS title shown here. */
private fun search(context: Context, query: String): List<IosSearchHit> {
    val term = query.trim().lowercase()
    val keys = SettingsActivity.settingsContainer.filter(term, IOS_SEARCHABLE_KEYS).map { it.key }.toSet()
    return searchHits.filter { (hitKeys, hit) ->
        val title = when (hit) {
            is IosSearchHit.Switch -> hit.item.title
            is IosSearchHit.Link -> hit.title
        }
        hitKeys.any { it in keys } || titleMatches(context.getString(title), term)
    }.map { it.second }
}

@Composable
fun IosMainScreen(needsEnabling: Boolean, onEnable: () -> Unit, navigate: (String) -> Unit) {
    val ctx = LocalContext.current
    val colors = LocalIosColors.current
    var query by rememberSaveable { mutableStateOf("") }
    val prefsVersion = rememberPrefsVersion()
    val languageCount = remember(prefsVersion) { SubtypeSettings.getEnabledSubtypes(true).map { it.locale() }.distinct().size }
    val font = remember(prefsVersion) { KeyboardFont.fromPref(ctx.prefs().getString(Settings.PREF_KEYBOARD_FONT, Defaults.PREF_KEYBOARD_FONT)) }
    val fontName = stringResource(font.title)
    val hits = remember(query) { if (query.isBlank()) emptyList() else search(ctx, query) }
    val noResults = stringResource(R.string.ios_no_results)
    val enableTitle = stringResource(R.string.ios_enable)
    val enableFooter = stringResource(R.string.ios_enable_footer)
    val keyboards = stringResource(R.string.ios_keyboards)
    val textReplacement = stringResource(R.string.ios_text_replacement)
    val appearance = stringResource(R.string.ios_appearance)
    val about = stringResource(R.string.ios_about)
    val allKeyboards = stringResource(R.string.ios_all_keyboards)
    val periodFooter = stringResource(R.string.ios_period_shortcut_footer)
    val feedback = stringResource(R.string.ios_feedback)

    IosScreen(
        title = stringResource(R.string.ios_app_name),
        onBack = null,
        bottomBar = { IosSearchField(query, { query = it }) },
    ) {
        if (query.isNotBlank()) {
            if (hits.isEmpty())
                iosGroup(footer = noResults, rows = emptyList())
            else
                iosGroup(rows = hits.map { hit ->
                    when (hit) {
                        is IosSearchHit.Switch -> switchRow(hit.item)
                        is IosSearchHit.Link -> { -> IosRow(stringResource(hit.title), onClick = { navigate(hit.route) }) }
                    }
                })
            return@IosScreen
        }
        if (needsEnabling)
            iosGroup(footer = enableFooter, rows = listOf {
                IosRow(enableTitle, icon = IosRowIcon(Color(0xFF34C759), text = "⌨"), titleColor = colors.tint, onClick = onEnable)
            })
        iosGroup(rows = listOf(
            { IosRow(keyboards, languageCount.toString(), IosRowIcon(Color(0xFF0A84FF), text = "⌨"), onClick = { navigate(IosRoute.Keyboards) }) },
            { IosRow(textReplacement, icon = IosRowIcon(Color(0xFFFF9F0A), text = "✎"), onClick = { navigate(IosRoute.TextReplacement) }) },
            { IosRow(appearance, fontName, IosRowIcon(Color(0xFFBF5AF2), text = "Aa"), onClick = { navigate(IosRoute.Appearance) }) },
        ))
        iosGroup(header = allKeyboards, footer = periodFooter, rows = allKeyboardsSwitches.map { switchRow(it) })
        iosGroup(header = feedback, rows = feedbackSwitches.map { switchRow(it) })
        iosGroup(rows = listOf {
            IosRow(about, icon = IosRowIcon(Color(0xFF8E8E93), text = "i"), onClick = { navigate(IosRoute.About) })
        })
    }
}

private fun switchRow(item: IosSwitchItem): @Composable () -> Unit = {
    IosSwitchRow(stringResource(item.title), item.key, item.default, if (item.reloadsKeyboard) { _ -> iosReloadKeyboard() } else null)
}
