// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.ios

/**
 * iOS-style suggestion bar: always at full height while predictive text is on (no rising when words appear),
 * hidden when it is off, leaving only the gap above the keys. Autofill and clipboard chips show it anyway.
 * Only the bar height changes, so the keys stay put.
 */
class IosStripHeight(
    private val hiddenPx: Int,
    private val shownPx: Int,
    private val apply: (Int) -> Unit,
) {
    private var shown = true
    private var external = false
    private var current = shownPx

    fun setShown(shown: Boolean) {
        this.shown = shown
        update()
    }

    /** an autofill or clipboard suggestion is in the bar */
    fun setExternal(external: Boolean) {
        if (external == this.external) return
        this.external = external
        update()
    }

    /** re-sends the current height, e.g. after the emoji or clipboard strip replaced the suggestions */
    fun reapply() = apply(current)

    private fun update() {
        current = if (shown || external) shownPx else hiddenPx
        apply(current)
    }
}
