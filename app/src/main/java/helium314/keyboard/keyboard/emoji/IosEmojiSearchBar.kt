// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.emoji

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import helium314.keyboard.keyboard.Keyboard
import helium314.keyboard.latin.R
import helium314.keyboard.latin.utils.ResourceUtils

/** iOS emoji search above the letters: a row of matching emoji, then the search field with the query. */
@SuppressLint("ViewConstructor")
internal class IosEmojiSearchBar(
    context: Context,
    private val textColor: Int,
    fieldColor: Int,
    caretColor: Int,
    val rowHeight: Int,
    emojiTextSize: Float,
    callback: EmojiViewCallback,
    private val onClose: () -> Unit,
) : LinearLayout(context) {
    private val dp = resources.displayMetrics.density * ResourceUtils.iosWidthScale(resources.displayMetrics)
    private val fieldHeight = (36 * dp).toInt()
    private val padding = (6 * dp).toInt()
    val totalHeight = rowHeight + fieldHeight + 2 * padding

    private val resultsView = EmojiPageKeyboardView(context, null).apply {
        setEmojiViewCallback(callback)
        setLabelTextSizeOverride(emojiTextSize)
        background = null
    }
    private val scroll = HorizontalScrollView(context).apply {
        isHorizontalScrollBarEnabled = false
        overScrollMode = OVER_SCROLL_NEVER
        addView(resultsView, LayoutParams(LayoutParams.WRAP_CONTENT, rowHeight))
    }
    private val noResults = TextView(context).apply {
        setText(R.string.ios_emoji_search_none)
        setTextColor(withAlpha(textColor, 0x8C))
        setTextSize(TypedValue.COMPLEX_UNIT_PX, 15 * dp)
        gravity = Gravity.CENTER
        visibility = GONE
    }
    private val queryView = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_PX, 17 * dp)
        isSingleLine = true
        ellipsize = android.text.TextUtils.TruncateAt.START
    }
    private val caret = View(context).apply { setBackgroundColor(caretColor) }
    private val blink = ObjectAnimator.ofFloat(caret, View.ALPHA, 1f, 1f, 0f, 0f).apply {
        duration = 1000
        repeatCount = ValueAnimator.INFINITE
    }

    init {
        orientation = VERTICAL
        setPadding((8 * dp).toInt(), padding, (8 * dp).toInt(), padding)
        addView(FrameLayout(context).apply {
            addView(scroll, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, rowHeight))
            addView(noResults, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, rowHeight))
        }, LayoutParams(LayoutParams.MATCH_PARENT, rowHeight))

        val field = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                cornerRadius = fieldHeight / 2f
                setColor(fieldColor)
            }
            setPadding((10 * dp).toInt(), 0, (4 * dp).toInt(), 0)
            addView(ImageView(context).apply {
                setImageResource(R.drawable.ic_ios_search)
                setColorFilter(withAlpha(textColor, 0x8C), PorterDuff.Mode.SRC_IN)
            }, LayoutParams((18 * dp).toInt(), (18 * dp).toInt()).apply { marginEnd = (6 * dp).toInt() })
            addView(queryView, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0f))
            addView(caret, LayoutParams((2 * dp).toInt().coerceAtLeast(2), (20 * dp).toInt()))
            addView(View(context), LayoutParams(0, 1, 1f))
            addView(ImageView(context).apply {
                setImageResource(R.drawable.ic_close)
                setColorFilter(withAlpha(textColor, 0x8C), PorterDuff.Mode.SRC_IN)
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                val p = (7 * dp).toInt()
                setPadding(p, p, p, p)
                contentDescription = context.getString(android.R.string.cancel)
                setOnClickListener { onClose() }
            }, LayoutParams(fieldHeight, fieldHeight))
        }
        addView(field, LayoutParams(LayoutParams.MATCH_PARENT, fieldHeight))
    }

    /** shows [query] in the field and [results] in the row; null results mean "nothing found" */
    fun show(query: String, results: Keyboard?) {
        if (query.isEmpty()) {
            queryView.setText(R.string.ios_emoji_search_hint)
            queryView.setTextColor(withAlpha(textColor, 0x8C))
            // the caret stands before the placeholder, as on iOS
            (caret.parent as LinearLayout).apply { removeView(caret); addView(caret, 1) }
        } else {
            queryView.text = query
            queryView.setTextColor(textColor)
            (caret.parent as LinearLayout).apply { removeView(caret); addView(caret, 2) }
        }
        noResults.visibility = if (results == null) VISIBLE else GONE
        scroll.visibility = if (results == null) INVISIBLE else VISIBLE
        if (results != null) resultsView.setKeyboard(results)
        scroll.scrollTo(0, 0)
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (isShown) blink.start() else blink.cancel()
    }

    override fun onDetachedFromWindow() {
        blink.cancel()
        super.onDetachedFromWindow()
    }

    private fun withAlpha(color: Int, alpha: Int) = (color and 0x00FFFFFF) or (alpha shl 24)
}
