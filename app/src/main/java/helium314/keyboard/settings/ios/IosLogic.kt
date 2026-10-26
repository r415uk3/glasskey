// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.ios

object IosLargeTitle {
    /** 0 while the large title is fully visible, 1 once it has scrolled away by its own height. */
    fun collapse(scrollPx: Int, titleHeightPx: Int): Float {
        if (titleHeightPx <= 0) return 0f
        return (scrollPx.toFloat() / titleHeightPx).coerceIn(0f, 1f)
    }
}

object IosSwitchLogic {
    /** A tap toggles; a drag settles on whichever end the thumb was released nearer to (0 = off, 1 = on). */
    fun checkedAfterRelease(startChecked: Boolean, dragFraction: Float, moved: Boolean): Boolean =
        if (moved) dragFraction >= 0.5f else !startChecked
}
