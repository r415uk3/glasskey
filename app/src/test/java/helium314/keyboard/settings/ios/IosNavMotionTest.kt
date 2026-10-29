// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.ios

import androidx.activity.BackEventCompat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IosNavMotionTest {
    @Test fun durationFollowsAnimationScale() {
        assertEquals(450, IosNavMotion.durationMillis(1f))
        assertEquals(900, IosNavMotion.durationMillis(2f))
        assertEquals(0, IosNavMotion.durationMillis(0f))
    }

    @Test fun easingIsFastOutDecelerating() {
        val e = IosNavMotion.Easing
        assertEquals(0f, e.transform(0f), 1e-4f)
        assertEquals(1f, e.transform(1f), 1e-4f)
        var last = 0f
        for (i in 1..100) {
            val v = e.transform(i / 100f)
            assertTrue(v >= last && v <= 1.0001f, "not monotonic at $i")
            last = v
        }
        assertTrue(e.transform(0.25f) > 0.4f) // most of the way early
        assertTrue(e.transform(0.75f) > 0.9f) // long gentle settle, no overshoot
    }

    @Test fun coveredPageDims() {
        assertEquals(0f, IosNavMotion.dimAlpha(0f, dark = false))
        assertEquals(0f, IosNavMotion.dimAlpha(0.5f, dark = false)) // the page on top is not dimmed
        assertEquals(0.12f, IosNavMotion.dimAlpha(-0.3f, dark = false), 1e-4f)
        assertEquals(0.25f, IosNavMotion.dimAlpha(-0.3f, dark = true), 1e-4f)
        assertEquals(0.06f, IosNavMotion.dimAlpha(-0.15f, dark = false), 1e-4f)
    }

    @Test fun pageOnTopCastsShadowWhileMoving() {
        assertEquals(0f, IosNavMotion.shadowAlpha(0f))
        assertEquals(0f, IosNavMotion.shadowAlpha(-0.2f))
        assertTrue(IosNavMotion.shadowAlpha(0.5f) > 0f)
        assertTrue(IosNavMotion.shadowAlpha(0.1f) < IosNavMotion.shadowAlpha(0.6f))
    }

    @Test fun sheetMotion() {
        assertEquals(250, IosSheetMotion.DurationMillis)
        assertEquals(0.9f, IosSheetMotion.InitialScale)
    }

    @Test fun backGestureFollowsTheFingersMovement() {
        val w = 1000f
        fun left(x: Float, p: Float) = BackEventCompat(x, 500f, p, BackEventCompat.EDGE_LEFT)
        fun right(x: Float, p: Float) = BackEventCompat(x, 500f, p, BackEventCompat.EDGE_RIGHT)
        fun pos(base: Float, start: Float, e: BackEventCompat, rtl: Boolean = false) = IosNavMotion.gesturePosition(base, start, e, w, rtl)
        // split screen: the window starts at 600 on screen; no jump at the start
        assertEquals(0f, pos(0f, 610f, left(610f, 0f)))
        // the system's progress is not where the finger is; the page moves as far as the finger
        assertEquals(0.3f, pos(0f, 610f, left(910f, 0.6f)), 1e-4f)
        // a page caught while sliding in moves on from where it is, never back
        assertEquals(0.5f, pos(0.4f, 0f, left(100f, 0.1f)), 1e-4f)
        assertEquals(0.4f, pos(0.4f, 100f, left(50f, 0.1f)), 1e-4f)
        assertEquals(1f, pos(0f, 0f, left(1500f, 1f)))
        // from the other edge, or without a position: the system's progress over the rest of the way
        assertEquals(0.4f, pos(0f, 900f, right(700f, 0.4f)), 1e-4f)
        assertEquals(0.5f, pos(0f, Float.NaN, left(Float.NaN, 0.5f)), 1e-4f)
        assertEquals(0f, pos(0f, 0f, left(Float.NaN, Float.NaN)))
        // right to left: mirrored
        assertEquals(0.3f, pos(0f, 990f, right(690f, 0.9f), rtl = true), 1e-4f)
    }
}
