// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.ios

import kotlin.test.Test
import kotlin.test.assertEquals

class IosStripHeightTest {
    /** records what would be animated; jumps straight to the target like a finished spring */
    private class FakeAnimator : IosStripHeight.Animator {
        val targets = mutableListOf<Int>()
        override fun animate(from: Int, to: Int, onFrame: (Int) -> Unit) { targets.add(to); onFrame(to) }
        override fun cancel() {}
    }

    private val applied = mutableListOf<Int>()
    private val animator = FakeAnimator()
    private val strip = IosStripHeight(collapsedPx = 8, expandedPx = 40, apply = { applied.add(it) }, animator = animator)

    @Test fun startsCollapsedWithoutAnimation() {
        assertEquals(listOf(8), applied)
        assertEquals(emptyList(), animator.targets)
    }

    @Test fun suggestionsExpandWithAnimation() {
        strip.update(hasContent = true)
        assertEquals(listOf(40), animator.targets)
        assertEquals(40, applied.last())
    }

    @Test fun sameStateDoesNotRestartTheSpring() {
        strip.update(hasContent = true)
        strip.update(hasContent = true)
        assertEquals(listOf(40), animator.targets)
    }

    @Test fun emptyStripCollapsesAgain() {
        strip.update(hasContent = true)
        strip.update(hasContent = false)
        assertEquals(listOf(40, 8), animator.targets)
        assertEquals(8, applied.last())
    }

    @Test fun onlyRealSuggestionsCount() {
        assertEquals(false, IosStripHeight.hasContent(isEmpty = true, isPunctuation = false))
        assertEquals(false, IosStripHeight.hasContent(isEmpty = false, isPunctuation = true))
        assertEquals(true, IosStripHeight.hasContent(isEmpty = false, isPunctuation = false))
    }
}
