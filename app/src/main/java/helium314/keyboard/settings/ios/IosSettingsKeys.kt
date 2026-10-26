// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.ios

import helium314.keyboard.latin.settings.Settings

/** Settings the iOS search may show: the kept settings that are entries of the SettingsContainer. */
val IOS_SEARCHABLE_KEYS: Set<String> = setOf(
    Settings.PREF_AUTO_CAP,
    Settings.PREF_AUTO_CORRECTION,
    Settings.PREF_SHOW_SUGGESTIONS,
    Settings.PREF_POPUP_ON,
    Settings.PREF_KEY_USE_DOUBLE_SPACE_PERIOD,
    Settings.PREF_SOUND_ON,
    Settings.PREF_VIBRATE_ON,
    Settings.PREF_KEYBOARD_FONT,
    Settings.PREF_SHOW_NUMBER_ROW,
    Settings.PREF_THEME_DAY_NIGHT,
    Settings.PREF_THEME_COLORS,
)
