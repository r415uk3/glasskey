// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard

import android.view.inputmethod.EditorInfo
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
import kotlin.test.assertTrue

/** On phones narrower than the 402pt reference, iOS shrinks keys and gaps with the width; so do we. */
@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowInputMethodManager2::class, ShadowProximityInfo::class], qualifiers = "w360dp-h800dp-xxhdpi")
class IosNarrowGeometryTest {
    private val latinIME = Robolectric.setupService(LatinIME::class.java)
    private val density = latinIME.resources.displayMetrics.density
    private val scale = 360f / 402f
    private fun dp(v: Float) = (v * density).roundToInt()
    private fun assertAbout(expected: Int, actual: Int) =
        assertTrue(abs(expected - actual) <= 2, "expected $expected ±2, got $actual")

    private fun keyboard(): Keyboard {
        val subtype = SubtypeUtilsAdditional.createEmojiCapableAdditionalSubtype(Locale.ENGLISH, "qwerty", true)
        return buildTestKeyboard(latinIME, EditorInfo(), subtype, KeyboardElement.ALPHABET, dp(360f),
            ResourceUtils.getKeyboardHeight(latinIME.resources, Settings.getValues())).first
    }

    @Test fun keysAndGapsScaleWithScreenWidth() {
        val kb = keyboard()
        val q = kb.sortedKeys.first { it.label == "q" }
        val w = kb.sortedKeys.first { it.label == "w" }
        val a = kb.sortedKeys.first { it.label == "a" }
        assertAbout(dp(45f * scale), q.height)
        assertAbout(dp(10f * scale), a.y - (q.y + q.height))
        assertAbout(dp(6.5f * scale), w.x - (q.x + q.width))
    }

    @Test fun keyboardIsShorterOnNarrowPhones() {
        assertAbout(dp(218f * scale), ResourceUtils.getKeyboardHeight(latinIME.resources, Settings.getValues()))
    }
}
