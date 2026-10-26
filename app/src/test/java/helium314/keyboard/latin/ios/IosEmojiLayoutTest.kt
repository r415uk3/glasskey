// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.ios

import helium314.keyboard.keyboard.emoji.EmojiCategory.Category
import kotlin.test.Test
import kotlin.test.assertEquals

class IosEmojiLayoutTest {
    @Test fun keysFillColumnsTopToBottom() {
        assertEquals(IosEmojiLayout.Cell(0, 0), IosEmojiLayout.cell(0, rows = 5))
        assertEquals(IosEmojiLayout.Cell(0, 4), IosEmojiLayout.cell(4, rows = 5))
        assertEquals(IosEmojiLayout.Cell(1, 2), IosEmojiLayout.cell(7, rows = 5))
    }

    @Test fun columnCountRoundsUp() {
        assertEquals(0, IosEmojiLayout.columnCount(0, rows = 5))
        assertEquals(1, IosEmojiLayout.columnCount(5, rows = 5))
        assertEquals(3, IosEmojiLayout.columnCount(12, rows = 5))
    }

    @Test fun titleSticksToTheLeftEdgeUntilTheSectionEnds() {
        // section fully visible: title at its start
        assertEquals(0, IosEmojiLayout.stickyTitleX(itemLeft = 40, itemWidth = 500, titleWidth = 120))
        // section scrolled 100px out to the left: title stays at the screen edge
        assertEquals(100, IosEmojiLayout.stickyTitleX(itemLeft = -100, itemWidth = 500, titleWidth = 120))
        // section nearly gone: title is pushed out with the section's end
        assertEquals(380, IosEmojiLayout.stickyTitleX(itemLeft = -450, itemWidth = 500, titleWidth = 120))
        // title wider than the section
        assertEquals(0, IosEmojiLayout.stickyTitleX(itemLeft = -10, itemWidth = 60, titleWidth = 120))
    }

    @Test fun currentSectionIsTheOneUnderTheAnchor() {
        val lefts = intArrayOf(-300, 200, 900)
        val rights = intArrayOf(180, 880, 1500)
        assertEquals(0, IosEmojiLayout.currentSection(lefts, rights, anchorX = 10))
        assertEquals(1, IosEmojiLayout.currentSection(lefts, rights, anchorX = 190)) // in the gap: next section
        assertEquals(2, IosEmojiLayout.currentSection(lefts, rights, anchorX = 950))
    }

    @Test fun sectionsFollowIosOrder() {
        assertEquals(
            listOf(
                listOf(Category.RECENTS),
                listOf(Category.SMILEYS, Category.PEOPLE),
                listOf(Category.NATURE),
                listOf(Category.FOOD),
                listOf(Category.ACTIVITIES),
                listOf(Category.TRAVEL_PLACES),
                listOf(Category.OBJECTS),
                listOf(Category.SYMBOLS),
                listOf(Category.FLAGS),
            ),
            IosEmojiLayout.sections(withFlags = true).map { it.categories }
        )
        assertEquals(false, IosEmojiLayout.sections(withFlags = false).any { Category.FLAGS in it.categories })
    }

    @Test fun deleteRepeatsAfterAPauseThenFast() {
        assertEquals(400L, IosEmojiLayout.deleteRepeatDelay(repeatsSoFar = 0))
        assertEquals(50L, IosEmojiLayout.deleteRepeatDelay(repeatsSoFar = 1))
        assertEquals(50L, IosEmojiLayout.deleteRepeatDelay(repeatsSoFar = 30))
    }
}
