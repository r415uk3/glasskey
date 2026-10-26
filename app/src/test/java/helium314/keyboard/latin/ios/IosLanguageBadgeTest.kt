// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.ios

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

class IosLanguageBadgeTest {
    @Test fun badgesLikeIos() {
        assertEquals("A", IosLanguageBadge.forLocale(Locale.US))
        assertEquals("Қ", IosLanguageBadge.forLocale(Locale.forLanguageTag("kk")))
        assertEquals("РУ", IosLanguageBadge.forLocale(Locale.forLanguageTag("ru")))
    }

    @Test fun otherLanguagesGetTwoLettersOfTheirOwnName() {
        assertEquals("DE", IosLanguageBadge.forLocale(Locale.GERMAN))
        assertEquals("УК", IosLanguageBadge.forLocale(Locale.forLanguageTag("uk")))
    }

    @Test fun badgeShowsAsTheNameFadesOut() {
        assertEquals(0, IosLanguageBadge.badgeAlpha(nameAlpha = 255))
        assertEquals(255, IosLanguageBadge.badgeAlpha(nameAlpha = 0))
    }
}
