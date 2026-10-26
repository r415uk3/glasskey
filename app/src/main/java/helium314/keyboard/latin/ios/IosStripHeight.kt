// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.ios

import androidx.dynamicanimation.animation.FloatValueHolder
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce

/**
 * iOS-style suggestion bar: collapsed while there is nothing to suggest (the panel top then sits where it
 * would with the bar hidden), raised with a spring when suggestions appear. Only the bar height changes,
 * so the keys stay where they are.
 */
class IosStripHeight @JvmOverloads constructor(
    private val collapsedPx: Int,
    private val expandedPx: Int,
    private val apply: (Int) -> Unit,
    private val animator: Animator = SpringAnimator(),
) {
    interface Animator {
        fun animate(from: Int, to: Int, onFrame: (Int) -> Unit)
        fun cancel()
    }

    private var expanded = false
    private var current = collapsedPx

    init {
        apply(collapsedPx)
    }

    /** re-sends the current height, e.g. after the emoji or clipboard strip replaced the suggestions */
    fun reapply() = apply(current)

    fun update(hasContent: Boolean) {
        if (hasContent == expanded) return
        expanded = hasContent
        animator.cancel()
        animator.animate(current, if (expanded) expandedPx else collapsedPx) {
            current = it
            apply(it)
        }
    }

    /** slightly bouncy, settles in ~350 ms */
    private class SpringAnimator : Animator {
        private var running: SpringAnimation? = null

        override fun animate(from: Int, to: Int, onFrame: (Int) -> Unit) {
            running = SpringAnimation(FloatValueHolder(from.toFloat())).apply {
                spring = SpringForce(to.toFloat()).setDampingRatio(0.72f).setStiffness(420f)
                addUpdateListener { _, value, _ -> onFrame(value.toInt()) }
                start()
            }
        }

        override fun cancel() {
            running?.cancel()
        }
    }

    companion object {
        /** punctuation-only "suggestions" are not shown in the iOS bar */
        fun hasContent(isEmpty: Boolean, isPunctuation: Boolean) = !isEmpty && !isPunctuation
    }
}
