// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.ios

/** iOS globe button under the keys: tap = next language in list order, long press = language list. */
class GlobeController(
    private val switchToNext: () -> Unit,
    private val showPicker: () -> Unit,
    private val keyFeedback: () -> Unit = {},
) {
    fun onTap() {
        keyFeedback() // vibration and click sound as set for the keys
        switchToNext()
    }

    fun onLongPress() = showPicker()
}
