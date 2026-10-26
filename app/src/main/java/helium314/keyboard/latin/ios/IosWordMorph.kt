// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.ios

import android.animation.ValueAnimator
import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import helium314.keyboard.latin.R

/**
 * iOS suggestion bar: when a suggestion changes, the letters it shares with the previous one stay put and the
 * changed letters fade in one after another.
 */
object IosWordMorph {
    const val STAGGER_MS = 22L
    const val FADE_MS = 140L

    fun commonPrefix(old: CharSequence, new: CharSequence): Int {
        var i = 0
        while (i < old.length && i < new.length && old[i] == new[i]) i++
        return i
    }

    fun durationMs(prefix: Int, newLength: Int): Long =
        if (newLength <= prefix) 0 else (newLength - prefix - 1) * STAGGER_MS + FADE_MS

    /** alpha (0..255) of each letter of [new], [elapsedMs] after the change */
    fun charAlphas(old: CharSequence, new: CharSequence, elapsedMs: Long): List<Int> {
        val prefix = commonPrefix(old, new)
        return new.indices.map { i ->
            if (i < prefix) 255
            else (((elapsedMs - (i - prefix) * STAGGER_MS).toFloat() / FADE_MS).coerceIn(0f, 1f) * 255).toInt()
        }
    }

    /** sets [new] on [view], animating the changed letters; the plain text is set again when done */
    @JvmStatic
    fun animate(view: TextView, new: CharSequence) {
        (view.getTag(R.id.ios_word_morph_animator) as? ValueAnimator)?.cancel()
        val old = view.text?.toString().orEmpty()
        val duration = durationMs(commonPrefix(old, new), new.length)
        view.text = new
        if (old.isEmpty() && new.isEmpty() || duration == 0L) return
        val color = view.currentTextColor
        val animator = ValueAnimator.ofFloat(0f, duration.toFloat()).apply {
            this.duration = duration
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                val elapsed = (it.animatedValue as Float).toLong()
                val spannable = SpannableString(new)
                charAlphas(old, new, elapsed).forEachIndexed { i, alpha ->
                    if (alpha < 255) spannable.setSpan(
                        ForegroundColorSpan(Color.argb(Color.alpha(color) * alpha / 255, Color.red(color), Color.green(color), Color.blue(color))),
                        i, i + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                view.text = spannable
            }
            doOnEndCompat { view.text = new }
        }
        view.setTag(R.id.ios_word_morph_animator, animator)
        animator.start()
    }

    private fun ValueAnimator.doOnEndCompat(action: () -> Unit) = addListener(object : android.animation.AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: android.animation.Animator) = action()
    })
}
