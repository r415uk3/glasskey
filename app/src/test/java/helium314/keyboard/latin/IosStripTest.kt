// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import helium314.keyboard.latin.suggestions.SuggestionStripView
import helium314.keyboard.latin.utils.ToolbarMode
import kotlin.test.Test
import kotlin.test.assertEquals

class IosStripTest {
    @Test fun iosNeverShowsExpandKeyEvenInIncognito() {
        assertEquals(false, SuggestionStripView.shouldShowStripExtras(isIos = true, ToolbarMode.SUGGESTION_STRIP, incognito = true))
        assertEquals(false, SuggestionStripView.shouldShowStripExtras(isIos = true, ToolbarMode.EXPANDABLE, incognito = false))
    }

    @Test fun iosWithoutIncognitoHasNoExtras() {
        assertEquals(false, SuggestionStripView.shouldShowStripExtras(isIos = true, ToolbarMode.SUGGESTION_STRIP, incognito = false))
    }

    @Test fun stockBehaviourPreserved() {
        assertEquals(true, SuggestionStripView.shouldShowStripExtras(isIos = false, ToolbarMode.EXPANDABLE, incognito = false))
        assertEquals(true, SuggestionStripView.shouldShowStripExtras(isIos = false, ToolbarMode.SUGGESTION_STRIP, incognito = true))
        assertEquals(false, SuggestionStripView.shouldShowStripExtras(isIos = false, ToolbarMode.SUGGESTION_STRIP, incognito = false))
    }
}
