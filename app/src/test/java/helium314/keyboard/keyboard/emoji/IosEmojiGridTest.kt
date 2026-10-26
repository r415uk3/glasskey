// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.emoji

import android.view.inputmethod.EditorInfo
import helium314.keyboard.ShadowInputMethodManager2
import helium314.keyboard.ShadowProximityInfo
import helium314.keyboard.buildTestKeyboard
import helium314.keyboard.keyboard.KeyboardElement
import helium314.keyboard.latin.LatinIME
import helium314.keyboard.latin.RichInputMethodSubtype
import helium314.keyboard.latin.utils.prefs
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

/** iOS emoji sections: keys fill 5-row columns top to bottom and the keyboard is as wide as its columns. */
@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowInputMethodManager2::class, ShadowProximityInfo::class])
class IosEmojiGridTest {
    private val latinIME = Robolectric.setupService(LatinIME::class.java)

    @Test fun keysFillColumnsAndKeyboardIsAsWideAsItsColumns() {
        val subtype = RichInputMethodSubtype.emojiSubtype.rawSubtype
        val smileys = buildTestKeyboard(latinIME, EditorInfo(), subtype, KeyboardElement.EMOJI_SMILEYS).first
        val template = buildTestKeyboard(latinIME, EditorInfo(), subtype, KeyboardElement.EMOJI_RECENTS).first
        val grid = DynamicGridKeyboard.ofIosColumns(latinIME.prefs(), template, false, 40, 30, 5, 100)
        smileys.sortedKeys.take(12).forEach { grid.addKeyLast(it) }

        val key7 = grid.sortedKeys[7]
        assertEquals(40, key7.x)
        assertEquals(60, key7.y)
        assertEquals(40, key7.width)
        assertEquals(30, key7.height)
        assertEquals(120, grid.mOccupiedWidth)
        assertEquals(150, grid.occupiedHeight)
    }
}
