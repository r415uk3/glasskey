// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.ios

/** iOS globe button under the keys: tap = next language in list order, long press = language list. */
class GlobeController(private val switchToNext: () -> Unit, private val showPicker: () -> Unit) {
    fun onTap() = switchToNext()

    fun onLongPress() = showPicker()
}
