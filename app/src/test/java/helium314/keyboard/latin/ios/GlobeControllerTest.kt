// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.ios

import helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode
import kotlin.test.Test
import kotlin.test.assertEquals

class GlobeControllerTest {
    private val codes = mutableListOf<Int>()
    private var pickerShown = 0
    private val controller = GlobeController(onCode = { codes.add(it) }, showPicker = { pickerShown++ })

    @Test fun tapSendsLanguageSwitch() {
        controller.onTap()
        assertEquals(listOf(KeyCode.LANGUAGE_SWITCH), codes)
        assertEquals(0, pickerShown)
    }

    @Test fun longPressOpensPicker() {
        controller.onLongPress()
        assertEquals(1, pickerShown)
        assertEquals(emptyList(), codes)
    }
}
