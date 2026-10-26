// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.ios

import android.content.SharedPreferences
import androidx.core.content.edit
import helium314.keyboard.keyboard.KeyboardTheme
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.settings.Settings

/** The iOS "Тема" choice, stored in the existing theme prefs. */
enum class IosThemeChoice {
    SYSTEM, LIGHT, DARK;

    companion object {
        fun read(prefs: SharedPreferences): IosThemeChoice = when {
            prefs.getBoolean(Settings.PREF_THEME_DAY_NIGHT, Defaults.PREF_THEME_DAY_NIGHT) -> SYSTEM
            prefs.getString(Settings.PREF_THEME_COLORS, Defaults.PREF_THEME_COLORS) == KeyboardTheme.THEME_IOS_DARK -> DARK
            else -> LIGHT
        }

        fun write(prefs: SharedPreferences, choice: IosThemeChoice) = prefs.edit {
            putBoolean(Settings.PREF_THEME_DAY_NIGHT, choice == SYSTEM)
            // SYSTEM uses the day colors by day, so they must be light again after a DARK choice
            putString(Settings.PREF_THEME_COLORS, if (choice == DARK) KeyboardTheme.THEME_IOS_DARK else KeyboardTheme.THEME_IOS_LIGHT)
            if (choice == SYSTEM) putString(Settings.PREF_THEME_COLORS_NIGHT, KeyboardTheme.THEME_IOS_DARK)
        }
    }
}
