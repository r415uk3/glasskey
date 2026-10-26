// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.ios

import kotlin.test.Test
import kotlin.test.assertEquals

class GlobeControllerTest {
    private var nextCalls = 0
    private var pickerShown = 0
    private val controller = GlobeController(switchToNext = { nextCalls++ }, showPicker = { pickerShown++ })

    @Test fun tapSwitchesToNextLanguage() {
        controller.onTap()
        assertEquals(1, nextCalls)
        assertEquals(0, pickerShown)
    }

    @Test fun longPressOpensPicker() {
        controller.onLongPress()
        assertEquals(1, pickerShown)
        assertEquals(0, nextCalls)
    }
}
