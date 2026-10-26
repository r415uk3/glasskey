// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.ios

import java.util.Locale

/** iOS shows a small language mark in the bottom right corner of the space bar once the language name faded out. */
object IosLanguageBadge {
    private val known = mapOf("en" to "A", "kk" to "Қ", "ru" to "РУ")

    @JvmStatic
    fun forLocale(locale: Locale): String =
        known[locale.language] ?: locale.getDisplayLanguage(locale).take(2).uppercase(locale)

    /** the badge fades in while the full language name fades out */
    @JvmStatic
    fun badgeAlpha(nameAlpha: Int): Int = 255 - nameAlpha.coerceIn(0, 255)
}
