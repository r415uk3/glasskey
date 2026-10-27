// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.ios

import kotlin.test.Test
import kotlin.test.assertEquals

class IosLogicTest {
    @Test fun titleCollapse() {
        assertEquals(0f, IosLargeTitle.collapse(0, 100))
        assertEquals(0.5f, IosLargeTitle.collapse(50, 100))
        assertEquals(1f, IosLargeTitle.collapse(400, 100))
        assertEquals(0f, IosLargeTitle.collapse(-20, 100))
    }

    @Test fun titleCollapseZeroHeight() = assertEquals(0f, IosLargeTitle.collapse(10, 0))

    // the large title is gone before it can show through the bar
    @Test fun largeTitleFadesOutEarly() {
        assertEquals(1f, IosLargeTitle.alpha(0f))
        assertEquals(0.5f, IosLargeTitle.alpha(0.3f), 0.001f)
        assertEquals(0f, IosLargeTitle.alpha(0.6f))
        assertEquals(0f, IosLargeTitle.alpha(1f))
    }

    // bar fully opaque down to where its fade starts, then fading to nothing at its bottom edge
    @Test fun scrollEdgeMask() {
        assertEquals(0.75f, IosScrollEdge.fadeStart(heightPx = 200f, fadePx = 50f))
        assertEquals(0f, IosScrollEdge.fadeStart(heightPx = 30f, fadePx = 50f))
        assertEquals(0f, IosScrollEdge.fadeStart(heightPx = 0f, fadePx = 50f))
    }

    @Test fun switchTapToggles() {
        assertEquals(true, IosSwitchLogic.checkedAfterRelease(false, 0f, moved = false))
        assertEquals(false, IosSwitchLogic.checkedAfterRelease(true, 1f, moved = false))
    }

    @Test fun switchDragDecidesBySide() {
        assertEquals(true, IosSwitchLogic.checkedAfterRelease(false, 0.8f, moved = true))
        assertEquals(false, IosSwitchLogic.checkedAfterRelease(true, 0.3f, moved = true))
        assertEquals(true, IosSwitchLogic.checkedAfterRelease(true, 0.7f, moved = true))
    }
}
