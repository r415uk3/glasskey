// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.ios

import androidx.compose.animation.core.TargetBasedAnimation
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
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

    private fun heldSlide(gate: IosNavGate) = TargetBasedAnimation(
        IosHeldSpec(gate, tween(IosNavMotion.DurationMillis, easing = IosNavMotion.Easing)),
        Float.VectorConverter, initialValue = 1f, targetValue = 0f,
    )
    private fun TargetBasedAnimation<Float, *>.at(ms: Long) = getValueFromNanos(ms * 1_000_000)

    // device frames of a push: the incoming page is composed, laid out and drawn in the first frame (190 ms), the
    // transition's clock runs during it; the next frames come every ~16 ms
    @Test fun slowFirstFrameDoesNotEatTheStartOfTheSlide() {
        val gate = IosNavGate()
        val slide = heldSlide(gate)
        assertEquals(1f, slide.at(0))          // first frame: page composed, still off screen
        gate.open()                            // ... and drawn
        assertEquals(1f, slide.at(190), 1e-4f) // first rendered frame after it: still at the start
        val second = slide.at(206)
        assertTrue(second < 1f && second > 0.9f, "16 ms in: $second") // the curve's first step, not 24% in
        assertEquals(1f - IosNavMotion.Easing.transform(225f / 450), slide.at(190 + 225), 1e-3f)
        assertEquals(0f, slide.at(190 + 450), 1e-4f)
        assertEquals(0f, slide.at(slide.durationNanos / 1_000_000))
    }

    @Test fun fastFirstFrameStartsRightAway() {
        val gate = IosNavGate()
        val slide = heldSlide(gate)
        assertEquals(1f, slide.at(0))
        gate.open()
        assertEquals(1f, slide.at(16), 1e-4f)
        assertEquals(1f - IosNavMotion.Easing.transform(16f / 450), slide.at(32), 1e-3f)
    }

    @Test fun heldAtMostMaxHold() {
        val slide = heldSlide(IosNavGate()) // the page never reports a draw
        assertEquals(1f, slide.at(IosNavMotion.MaxHoldMillis.toLong()), 1e-4f)
        assertEquals(0f, slide.at(IosNavMotion.MaxHoldMillis + 450L), 1e-4f)
        assertEquals((450L + IosNavMotion.MaxHoldMillis) * 1_000_000, slide.durationNanos)
    }

    @Test fun oneGatePerTransition() {
        val clock = IosNavClock()
        clock.onPageDrawn("main")
        val push = clock.gate("main", "keyboards")
        assertTrue(!push.isOpen)
        assertTrue(clock.gate("main", "keyboards") === push) // asked again by the other page of the same push
        clock.onPageDrawn("keyboards")
        assertTrue(push.isOpen)
        assertTrue(clock.gate("keyboards", "main").isOpen) // back during the push, to a page that is still shown
        clock.onPageGone("main")
        assertTrue(!clock.gate("keyboards", "other").isOpen) // a page not composed yet waits for its first layout
        clock.onPageGone("keyboards")
        assertTrue(!clock.gate("other", "keyboards").isOpen) // composed again later: waits again
    }
}
