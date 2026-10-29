// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.ios

import kotlin.test.Test
import kotlin.test.assertEquals

class IosStripHeightTest {
    private val applied = mutableListOf<Int>()
    private val strip = IosStripHeight(hiddenPx = 8, shownPx = 40, apply = { applied.add(it) })

    @Test fun shownStripHasFullHeightRightAway() {
        strip.setShown(true)
        assertEquals(listOf(40), applied)
    }

    @Test fun hiddenStripKeepsOnlyTheGapAboveTheKeys() {
        strip.setShown(false)
        assertEquals(listOf(8), applied)
    }

    // password-manager autofill and the clipboard code chip show even where predictive text is off
    @Test fun externalSuggestionsShowAHiddenStrip() {
        strip.setShown(false)
        strip.setExternal(true)
        strip.setExternal(false)
        assertEquals(listOf(8, 40, 8), applied)
    }

    @Test fun reapplyResendsTheCurrentHeight() {
        strip.setShown(true)
        strip.reapply()
        assertEquals(listOf(40, 40), applied)
    }
}
