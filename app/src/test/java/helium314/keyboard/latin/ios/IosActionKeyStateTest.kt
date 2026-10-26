// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.ios

import kotlin.test.Test
import kotlin.test.assertEquals

class IosActionKeyStateTest {
    @Test fun emptyFieldMakesTheActionKeyIdle() {
        val state = IosActionKeyState()
        assertEquals(true, state.update(textBeforeCursor = "", textAfterCursor = ""))
        assertEquals(true, state.idle)
    }

    @Test fun anyTextActivatesIt() {
        val state = IosActionKeyState()
        state.update("", "")
        assertEquals(true, state.update("h", ""))
        assertEquals(false, state.idle)
        state.update("", "ello") // text after the cursor counts too
        assertEquals(false, state.idle)
    }

    @Test fun reportsOnlyRealChanges() {
        val state = IosActionKeyState()
        state.update("a", "")
        assertEquals(false, state.update("ab", ""))
    }

    @Test fun unknownTextCountsAsNotEmpty() {
        // input connection unavailable: keep the key usable
        val state = IosActionKeyState()
        state.update(null, null)
        assertEquals(false, state.idle)
    }
}
