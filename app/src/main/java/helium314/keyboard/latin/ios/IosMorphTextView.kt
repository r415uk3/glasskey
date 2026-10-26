// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.ios

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.util.AttributeSet
import android.view.animation.PathInterpolator
import android.widget.TextView
import helium314.keyboard.latin.ios.IosWordMorph.Glyph

/** Suggestion word that can morph from the previous word letter by letter (iOS style), see [IosWordMorph]. */
class IosMorphTextView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : TextView(context, attrs, defStyleAttr) {
    private var animator: ValueAnimator? = null
    private var glyphs: List<Glyph> = emptyList()
    private var oldOffsets = FloatArray(0)
    private var newOffsets = FloatArray(0)
    private var oldWidth = 0f
    private var newWidth = 0f
    private var progress = 1f

    fun morph(old: String, new: CharSequence) {
        animator?.cancel()
        text = new
        val newText = new.toString()
        if (old == newText || old.isEmpty() || newText.isEmpty()) return
        glyphs = IosWordMorph.plan(old, newText)
        oldOffsets = IosWordMorph.offsets(old, paint)
        newOffsets = IosWordMorph.offsets(newText, paint)
        oldWidth = paint.measureText(old)
        newWidth = paint.measureText(newText)
        progress = 0f
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = IosWordMorph.DURATION_MS
            interpolator = PathInterpolator(0.25f, 0.1f, 0.25f, 1f) // ease, like UIKit's default curve
            addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    progress = 1f
                    glyphs = emptyList()
                    invalidate()
                }
            })
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (glyphs.isEmpty() || progress >= 1f) {
            super.onDraw(canvas)
            return
        }
        val color = currentTextColor
        // single-line TextViews may draw with a large horizontal scroll; the canvas is translated by it
        val center = scrollX + (width + paddingLeft - paddingRight) / 2f
        val oldStart = center - oldWidth / 2
        val newStart = center - newWidth / 2
        val baseline = baseline.toFloat()
        for (glyph in glyphs) {
            paint.color = color
            paint.alpha = IosWordMorph.glyphAlpha(glyph, progress) * Color.alpha(color) / 255
            canvas.drawText(glyph.char.toString(), IosWordMorph.glyphX(glyph, oldStart, newStart, oldOffsets, newOffsets, progress), baseline, paint)
        }
        paint.color = color
    }
}
