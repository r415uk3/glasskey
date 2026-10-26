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
