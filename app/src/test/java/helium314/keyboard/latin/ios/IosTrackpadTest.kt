// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.ios

import helium314.keyboard.latin.common.Constants
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class IosTrackpadTest {
    @AfterTest fun reset() { IosTrackpad.active = false }

    @Test fun longPressOnSpaceStartsTheTrackpadInIosStyle() {
        assertEquals(true, IosTrackpad.shouldStartOnLongPress(isIos = true, code = Constants.CODE_SPACE, hasPopupKeys = false))
    }

    @Test fun notOnOtherKeysOrStyles() {
        assertEquals(false, IosTrackpad.shouldStartOnLongPress(isIos = true, code = 'a'.code, hasPopupKeys = false))
        assertEquals(false, IosTrackpad.shouldStartOnLongPress(isIos = false, code = Constants.CODE_SPACE, hasPopupKeys = false))
        // a space with its own popup keys (some layouts) keeps the popup
        assertEquals(false, IosTrackpad.shouldStartOnLongPress(isIos = true, code = Constants.CODE_SPACE, hasPopupKeys = true))
    }

    @Test fun labelsAreHiddenOnlyWhileActive() {
        assertEquals(false, IosTrackpad.hidesKeyLabels())
        IosTrackpad.active = true
        assertEquals(true, IosTrackpad.hidesKeyLabels())
    }
}
