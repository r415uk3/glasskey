// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.ios

/**
 * iOS-style suggestion bar: always at full height while predictive text is on (no rising when words appear),
 * hidden when it is off, leaving only the gap above the keys. Only the bar height changes, so the keys stay put.
 */
class IosStripHeight(
    private val hiddenPx: Int,
    private val shownPx: Int,
    private val apply: (Int) -> Unit,
) {
    private var current = shownPx

    fun setShown(shown: Boolean) {
        current = if (shown) shownPx else hiddenPx
        apply(current)
    }

    /** re-sends the current height, e.g. after the emoji or clipboard strip replaced the suggestions */
    fun reapply() = apply(current)
}
