// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.ios

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GlobeCycleTest {
    private val cycle = GlobeCycle<String>(windowMs = 1000)
    private val langs = listOf("en", "kk", "ru")
    private var current = "en"
    private var now = 0L

    // one globe tap a little later than the last one; switches like LatinIME would
    private fun tap(after: Long = 5000): String? {
        now += after
        val target = cycle.next(current, langs, now) ?: return null
        current = target
        cycle.onShown(target)
        return target
    }

    @Test fun singleTapsToggleBetweenTheLastTwoLanguages() {
        cycle.onShown("en")
        assertEquals("kk", tap()) // nothing used before: next in list
        assertEquals("en", tap())
        assertEquals("kk", tap())
    }

    @Test fun quickTapsCycleThroughAllLanguagesFromMostRecent() {
        cycle.onShown("ru"); cycle.onShown("en") // ru was used before en
        assertEquals("ru", tap())
        assertEquals("kk", tap(after = 400))
        assertEquals("en", tap(after = 400))
        assertEquals("ru", tap(after = 400))
    }

    @Test fun afterCyclingASingleTapReturnsToWhereTheCycleStarted() {
        cycle.onShown("ru"); cycle.onShown("en")
        tap(); tap(after = 400) // en -> ru -> kk
        assertEquals("kk", current)
        assertEquals("en", tap())
        assertEquals("kk", tap())
    }

    @Test fun languageChosenElsewhereBecomesTheLastUsed() {
        cycle.onShown("en")
        current = "ru"; cycle.onShown("ru") // picked from the long-press list
        assertEquals("en", tap())
        assertEquals("ru", tap())
    }

    @Test fun removedLanguagesAreSkipped() {
        cycle.onShown("kk"); cycle.onShown("en")
        now += 5000
        assertEquals("ru", cycle.next("en", listOf("en", "ru"), now))
    }

    // the current language was just turned off: a tap goes to the last used one that is still on
    @Test fun currentLanguageNotEnabledGoesToTheLastUsed() {
        cycle.onShown("ru"); cycle.onShown("kk"); cycle.onShown("en")
        now += 5000
        assertEquals("kk", cycle.next("en", listOf("kk", "ru"), now))
    }

    @Test fun nothingToSwitchToWithOneLanguage() {
        assertNull(cycle.next("en", listOf("en"), 0))
    }
}
