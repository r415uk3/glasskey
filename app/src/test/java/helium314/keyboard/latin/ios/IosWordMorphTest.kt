// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.ios

import helium314.keyboard.latin.ios.IosWordMorph.Glyph
import kotlin.test.Test
import kotlin.test.assertEquals

class IosWordMorphTest {
    @Test fun lettersInBothWordsStayAndOnlyNewOnesAreAdded() {
        val plan = IosWordMorph.plan("при", "привет")
        assertEquals(listOf(Glyph('п', 0, 0), Glyph('р', 1, 1), Glyph('и', 2, 2), Glyph('в', null, 3), Glyph('е', null, 4), Glyph('т', null, 5)), plan)
    }

    @Test fun sharedLettersAnywhereInTheWordAreKept() {
        // "кот" -> "крот": к, о, т stay (moving), р is new
        val plan = IosWordMorph.plan("кот", "крот")
        assertEquals(listOf(Glyph('к', 0, 0), Glyph('р', null, 1), Glyph('о', 1, 2), Glyph('т', 2, 3)), plan)
    }

    @Test fun removedLettersFadeOutWhereTheyWere() {
        val plan = IosWordMorph.plan("принцип", "привет")
        assertEquals(3, plan.count { it.from != null && it.to != null })
        assertEquals("нцип".toList(), plan.filter { it.to == null }.map { it.char })
        assertEquals("вет".toList(), plan.filter { it.from == null }.map { it.char })
    }

    @Test fun keptLettersSlideToTheRecenteredWord() {
        // widths 10 each, view 100 wide: "при" starts at 35, "привет" at 20
        val glyph = Glyph('п', 0, 0)
        val x = IosWordMorph.glyphX(glyph, oldStart = 35f, newStart = 20f, oldOffsets = floatArrayOf(0f, 10f, 20f), newOffsets = floatArrayOf(0f, 10f, 20f, 30f, 40f, 50f), progress = 0.5f)
        assertEquals(27.5f, x)
    }

    @Test fun newLettersFadeInAndRemovedOnesFadeOut() {
        assertEquals(0, IosWordMorph.glyphAlpha(Glyph('в', null, 3), progress = 0f))
        assertEquals(255, IosWordMorph.glyphAlpha(Glyph('в', null, 3), progress = 1f))
        assertEquals(255, IosWordMorph.glyphAlpha(Glyph('н', 3, null), progress = 0f))
        assertEquals(0, IosWordMorph.glyphAlpha(Glyph('н', 3, null), progress = 1f))
        assertEquals(255, IosWordMorph.glyphAlpha(Glyph('п', 0, 0), progress = 0.5f))
    }
}
