// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard

import android.graphics.Paint
import android.view.inputmethod.EditorInfo
import helium314.keyboard.keyboard.KeyboardElement
import helium314.keyboard.keyboard.PopupKeysKeyboard
import helium314.keyboard.latin.LatinIME
import helium314.keyboard.latin.utils.SubtypeUtilsAdditional
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** iOS long-press panel: the letter itself comes first, then its variants, in key-sized cells. */
@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowInputMethodManager2::class, ShadowProximityInfo::class])
class IosPopupTest {
    private val latinIME = Robolectric.setupService(LatinIME::class.java)

    private fun popupFor(label: String, isSingleWithPreview: Boolean = true): Pair<Int, PopupKeysKeyboard> {
        val subtype = SubtypeUtilsAdditional.createEmojiCapableAdditionalSubtype(Locale.forLanguageTag("ru"), "russian", true)
        val keyboard = buildTestKeyboard(latinIME, EditorInfo(), subtype, KeyboardElement.ALPHABET).first
        val key = keyboard.sortedKeys.first { it.label == label }
        // the key preview is much taller than a key in the iOS style (balloon), which must not size the panel
        val popup = PopupKeysKeyboard.Builder(latinIME, key, keyboard, isSingleWithPreview, key.width + 50, key.height * 2, Paint()).build()
        return key.height to popup
    }

    @Test fun baseLetterComesFirst() {
        val (_, popup) = popupFor("е")
        val labels = popup.sortedKeys.sortedBy { it.x }.map { it.label }
        assertEquals("е", labels.first(), labels.toString())
        assertTrue("ё" in labels, labels.toString())
    }

    @Test fun baseLetterSitsAboveTheFinger() {
        // the popup is positioned so that its default key is above the pressed key; on iOS that is the letter itself
        val (_, popup) = popupFor("е")
        val base = popup.sortedKeys.first { it.label == "е" }
        assertTrue(popup.defaultCoordX in base.x..(base.x + base.width), "default x ${popup.defaultCoordX}, base ${base.x}..${base.x + base.width}")
    }

    @Test fun cellsSitSymmetricallyInThePanel() {
        val (_, popup) = popupFor("е")
        val keys = popup.sortedKeys.sortedBy { it.x }
        val left = keys.first().x
        val right = popup.mOccupiedWidth - (keys.last().x + keys.last().width)
        val top = keys.first().y
        val bottom = popup.mOccupiedHeight - (keys.first().y + keys.first().height)
        assertTrue(kotlin.math.abs(left - right) <= 1, "left $left right $right")
        assertTrue(kotlin.math.abs(top - bottom) <= 1, "top $top bottom $bottom")
    }

    @Test fun cellsAreKeySizedNotPreviewSized() {
        val (keyHeight, popup) = popupFor("е")
        assertTrue(popup.sortedKeys.all { it.height <= keyHeight }, "${popup.sortedKeys.map { it.height }} vs key $keyHeight")
    }
}
