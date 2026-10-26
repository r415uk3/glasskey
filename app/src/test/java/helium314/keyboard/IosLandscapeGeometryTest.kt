// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard

import android.view.inputmethod.EditorInfo
import helium314.keyboard.keyboard.Key
import helium314.keyboard.keyboard.Keyboard
import helium314.keyboard.keyboard.KeyboardElement
import helium314.keyboard.latin.LatinIME
import helium314.keyboard.latin.R
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

/** landscape: iOS keys are much lower (about half the screen with the bars), not a 46% block plus bars */
@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowInputMethodManager2::class, ShadowProximityInfo::class], qualifiers = "w800dp-h360dp-land-xxhdpi")
class IosLandscapeGeometryTest {
    private val latinIME = Robolectric.setupService(LatinIME::class.java)
    private val density = latinIME.resources.displayMetrics.density
    private fun dp(v: Float) = (v * density).roundToInt()
    private fun assertAbout(expected: Int, actual: Int) =
        assertTrue(abs(expected - actual) <= 2, "expected $expected ±2, got $actual")

    private fun keyboard(): Keyboard {
        val subtype = SubtypeUtilsAdditional.createEmojiCapableAdditionalSubtype(Locale.ENGLISH, "qwerty", true)
        val height = ResourceUtils.getKeyboardHeight(latinIME.resources, Settings.getValues())
        return buildTestKeyboard(latinIME, EditorInfo(), subtype, KeyboardElement.ALPHABET, dp(800f), height).first
    }

    private fun Keyboard.key(label: String): Key = sortedKeys.first { it.label == label }

    @Test fun keyboardIsLow() {
        // 6 top + 4 rows of 30 + 3 gaps of 5
        assertAbout(dp(141f), ResourceUtils.getKeyboardHeight(latinIME.resources, Settings.getValues()))
    }

    @Test fun rowsAre30dpWith5dpGaps() {
        val kb = keyboard()
        assertAbout(dp(30f), kb.key("q").height)
        assertAbout(dp(35f), kb.key("a").y - kb.key("q").y)
    }

    @Test fun bottomBarIsSmaller() {
        assertAbout(dp(32f), latinIME.resources.getDimensionPixelSize(R.dimen.config_ios_bottom_bar_height))
    }
}
