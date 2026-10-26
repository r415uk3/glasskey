// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.ios

import android.widget.TextView

/**
 * iOS suggestion bar: when a suggestion changes, letters found in both words stay and slide to their place in the
 * re-centered new word, new letters fade in, letters that are gone fade out where they were.
 */
object IosWordMorph {
    const val DURATION_MS = 260L

    /** a letter of the old word ([from]) and/or the new word ([to]); null = not in that word */
    data class Glyph(val char: Char, val from: Int?, val to: Int?)

    /** letters of [old] and [new] matched by longest common subsequence, in display order */
    fun plan(old: CharSequence, new: CharSequence): List<Glyph> {
        val lcs = Array(old.length + 1) { IntArray(new.length + 1) }
        for (i in old.indices.reversed()) for (j in new.indices.reversed())
            lcs[i][j] = if (old[i] == new[j]) lcs[i + 1][j + 1] + 1 else maxOf(lcs[i + 1][j], lcs[i][j + 1])
        val glyphs = mutableListOf<Glyph>()
        var i = 0
        var j = 0
        while (i < old.length || j < new.length) {
            when {
                i < old.length && j < new.length && old[i] == new[j] -> glyphs.add(Glyph(new[j], i++, j++))
                j >= new.length || (i < old.length && lcs[i + 1][j] >= lcs[i][j + 1]) -> glyphs.add(Glyph(old[i], i++, null))
                else -> glyphs.add(Glyph(new[j], null, j++))
            }
        }
        return glyphs
    }

    /** x of a glyph at [progress] (0..1); offsets are each letter's distance from the start of its word */
    fun glyphX(glyph: Glyph, oldStart: Float, newStart: Float, oldOffsets: FloatArray, newOffsets: FloatArray, progress: Float): Float {
        val fromX = glyph.from?.let { oldStart + oldOffsets[it] }
        val toX = glyph.to?.let { newStart + newOffsets[it] }
        return when {
            fromX != null && toX != null -> fromX + (toX - fromX) * progress
            toX != null -> toX
            else -> fromX!!
        }
    }

    fun glyphAlpha(glyph: Glyph, progress: Float): Int = when {
        glyph.from != null && glyph.to != null -> 255
        glyph.to != null -> (255 * progress).toInt()
        else -> (255 * (1 - progress)).toInt()
    }

    /** sets [new] on [view], morphing from [old] when the view supports it */
    @JvmStatic
    fun animate(view: TextView, old: String, new: CharSequence) {
        if (view is IosMorphTextView) view.morph(old, new) else view.text = new
    }

    fun offsets(text: String, paint: android.graphics.Paint): FloatArray {
        val widths = FloatArray(text.length)
        paint.getTextWidths(text, widths)
        val offsets = FloatArray(text.length)
        for (i in 1 until text.length) offsets[i] = offsets[i - 1] + widths[i - 1]
        return offsets
    }
}
