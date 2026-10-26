// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.ios

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IosWordMorphTest {
    @Test fun sharedPrefixStaysVisible() {
        val alphas = IosWordMorph.charAlphas("при", "привет", elapsedMs = 0)
        assertEquals(listOf(255, 255, 255), alphas.take(3))
        assertTrue(alphas.drop(3).all { it == 0 }, alphas.toString())
    }

    @Test fun changedLettersAppearOneAfterAnother() {
        val changed = IosWordMorph.charAlphas("при", "привет", elapsedMs = IosWordMorph.FADE_MS / 2).drop(3)
        assertTrue(changed.first() > 0, changed.toString())
        assertTrue(changed.zipWithNext().all { (a, b) -> a > b || b == 0 }, changed.toString())
    }

    @Test fun everythingVisibleWhenFinished() {
        val new = "привет"
        val alphas = IosWordMorph.charAlphas("", new, IosWordMorph.durationMs(0, new.length))
        assertTrue(alphas.all { it == 255 }, alphas.toString())
    }

    @Test fun identicalWordsDoNotAnimate() {
        assertEquals(0L, IosWordMorph.durationMs(IosWordMorph.commonPrefix("hello", "hello"), 5))
    }
}
