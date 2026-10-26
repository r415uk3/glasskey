// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard

import android.view.inputmethod.EditorInfo
import helium314.keyboard.keyboard.Key
import helium314.keyboard.keyboard.Keyboard
import helium314.keyboard.keyboard.KeyboardElement
import helium314.keyboard.latin.LatinIME
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.ResourceUtils
import helium314.keyboard.latin.utils.SubtypeUtilsAdditional
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowInputMethodManager2::class, ShadowProximityInfo::class], qualifiers = "w360dp-h800dp-xxhdpi")
class IosGeometryTest {
    private val latinIME = Robolectric.setupService(LatinIME::class.java)
    private val density = latinIME.resources.displayMetrics.density
    private fun dp(v: Float) = (v * density).roundToInt()

    private fun keyboard(height: Int = ResourceUtils.getKeyboardHeight(latinIME.resources, Settings.getValues())): Keyboard {
        val subtype = SubtypeUtilsAdditional.createEmojiCapableAdditionalSubtype(Locale.ENGLISH, "qwerty", true)
        return buildTestKeyboard(latinIME, EditorInfo(), subtype, KeyboardElement.ALPHABET, dp(402f), height).first
    }

    private fun Keyboard.key(label: String): Key = sortedKeys.first { it.label == label }

    private fun assertAbout(expected: Int, actual: Int) =
        assertTrue(abs(expected - actual) <= 1, "expected $expected ±1, got $actual")

    @Test fun defaultHeightFitsFourRowsOf45dp() {
        // 8 top padding + 4 rows of 45 + 3 gaps of 10
        assertAbout(dp(218f), ResourceUtils.getKeyboardHeight(latinIME.resources, Settings.getValues()))
    }

    @Test fun keyHeightIs45dp() {
        val kb = keyboard()
        for (label in listOf("q", "a", "z")) assertAbout(dp(45f), kb.key(label).height)
    }

    @Test fun horizontalGapIs6_5dp() {
        val kb = keyboard()
        val q = kb.key("q"); val w = kb.key("w")
        assertAbout(dp(6.5f), w.x - (q.x + q.width))
    }

    @Test fun rowGapIs10dp() {
        val kb = keyboard()
        val q = kb.key("q"); val a = kb.key("a")
        assertAbout(dp(10f), a.y - (q.y + q.height))
    }

    @Test fun landscapeShrinksNotClips() {
        val height = dp(150f) // less than the 218dp design height
        val kb = keyboard(height)
        val keys = kb.sortedKeys.filter { !it.isSpacer }
        assertTrue(keys.all { it.height > 0 })
        assertTrue(keys.maxOf { it.y + it.height } <= height, "bottom ${keys.maxOf { it.y + it.height }} > $height")
        assertEquals(keys.first { it.label == "q" }.height, keys.first { it.label == "a" }.height)
    }
}
