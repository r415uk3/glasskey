// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.ios

/** iOS: the action key (go, search, send...) stays grey until the field contains text. */
class IosActionKeyState {
    var idle = false
        private set

    /** @return whether [idle] changed, i.e. the key needs to be redrawn */
    fun update(textBeforeCursor: CharSequence?, textAfterCursor: CharSequence?): Boolean {
        // unknown text (no input connection) keeps the key usable
        val empty = textBeforeCursor != null && textAfterCursor != null && textBeforeCursor.isEmpty() && textAfterCursor.isEmpty()
        if (empty == idle) return false
        idle = empty
        return true
    }

    companion object {
        @JvmField val instance = IosActionKeyState()
    }
}
