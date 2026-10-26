// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.ios

import helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode

/** iOS globe button under the keys: tap = next language, long press = language list. */
class GlobeController(private val onCode: (Int) -> Unit, private val showPicker: () -> Unit) {
    fun onTap() = onCode(KeyCode.LANGUAGE_SWITCH)

    fun onLongPress() = showPicker()
}
