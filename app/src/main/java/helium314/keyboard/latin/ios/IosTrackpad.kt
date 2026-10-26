// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.ios

import helium314.keyboard.latin.common.Constants

/** iOS: long-press on space turns the keyboard into a cursor trackpad, the keys lose their labels meanwhile. */
object IosTrackpad {
    @JvmField @Volatile var active = false

    @JvmStatic
    fun shouldStartOnLongPress(isIos: Boolean, code: Int, hasPopupKeys: Boolean) =
        isIos && code == Constants.CODE_SPACE && !hasPopupKeys

    @JvmStatic
    fun hidesKeyLabels() = active
}
