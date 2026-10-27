// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.ios

object IosLargeTitle {
    /** 0 while the large title is fully visible, 1 once it has scrolled away by its own height. */
    fun collapse(scrollPx: Int, titleHeightPx: Int): Float {
        if (titleHeightPx <= 0) return 0f
        return (scrollPx.toFloat() / titleHeightPx).coerceIn(0f, 1f)
    }

    /** Large title opacity: fully gone by 60 % [collapse], before much of it is under the top bar. */
    fun alpha(collapse: Float): Float = (1f - collapse / 0.6f).coerceIn(0f, 1f)
}

object IosScrollEdge {
    /** Fraction of the bar height where its bottom fade (of [fadePx]) starts; 0 if the bar is shorter than the fade. */
    fun fadeStart(heightPx: Float, fadePx: Float): Float =
        if (heightPx <= 0f) 0f else ((heightPx - fadePx) / heightPx).coerceAtLeast(0f)
}

object IosSwitchLogic {
    /** A tap toggles; a drag settles on whichever end the thumb was released nearer to (0 = off, 1 = on). */
    fun checkedAfterRelease(startChecked: Boolean, dragFraction: Float, moved: Boolean): Boolean =
        if (moved) dragFraction >= 0.5f else !startChecked
}
