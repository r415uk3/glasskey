// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.utils

import androidx.annotation.FontRes
import androidx.annotation.StringRes
import helium314.keyboard.latin.R

/** Keyboard font choices bundled with the app (all SIL OFL 1.1, see assets/licenses). [SYSTEM] keeps the default typeface. */
enum class KeyboardFont(val prefValue: String, @FontRes val regular: Int?, @StringRes val title: Int) {
    SYSTEM("system", null, R.string.keyboard_font_system),
    INTER("inter", R.font.inter_regular, R.string.keyboard_font_inter),
    GOLOS_TEXT("golos_text", R.font.golos_text_regular, R.string.keyboard_font_golos_text),
    ONEST("onest", R.font.onest_regular, R.string.keyboard_font_onest),
    NOTO_SANS("noto_sans", R.font.noto_sans_regular, R.string.keyboard_font_noto_sans);

    companion object {
        fun fromPref(value: String?): KeyboardFont = entries.firstOrNull { it.prefValue == value } ?: INTER
    }
}
