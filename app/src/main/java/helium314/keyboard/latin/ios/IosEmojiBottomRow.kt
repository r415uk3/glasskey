// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.ios

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import helium314.keyboard.latin.R
import helium314.keyboard.latin.utils.ResourceUtils

/** iOS emoji screen bottom row: ABC, one icon per section (the current one on a round highlight), delete. */
@SuppressLint("ViewConstructor")
internal class IosEmojiBottomRow(
    context: Context,
    alphabetLabel: String,
    sections: List<IosEmojiLayout.Section>,
    private val textColor: Int,
    private val onAlphabet: () -> Unit,
    private val onSection: (Int) -> Unit,
    private val onDeleteDown: () -> Unit,
    private val onDeleteRepeat: (Int) -> Unit,
    private val onDeleteUp: () -> Unit,
) : LinearLayout(context) {
    private val dp = resources.displayMetrics.density * ResourceUtils.iosWidthScale(resources.displayMetrics)
    private val icons = ArrayList<ImageView>()
    private var deleteRepeats = 0
    private val repeatDelete = object : Runnable {
        override fun run() {
            deleteRepeats++
            onDeleteRepeat(deleteRepeats)
            postDelayed(this, IosEmojiLayout.deleteRepeatDelay(deleteRepeats))
        }
    }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding((8 * dp).toInt(), 0, (8 * dp).toInt(), 0)

        addView(TextView(context).apply {
            text = alphabetLabel
            setTextColor(textColor)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, 15 * dp)
            gravity = Gravity.CENTER
            setOnClickListener { onAlphabet() }
        }, LayoutParams((44 * dp).toInt(), LayoutParams.MATCH_PARENT))

        val iconSize = (27 * dp).toInt()
        sections.forEachIndexed { i, section ->
            val icon = ImageView(context).apply {
                setImageResource(section.icon)
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                val pad = (6 * dp).toInt()
                setPadding(pad, pad, pad, pad)
                contentDescription = context.getString(section.title)
                setOnClickListener { onSection(i) }
            }
            icons.add(icon)
            addView(LinearLayout(context).apply {
                gravity = Gravity.CENTER
                addView(icon, LayoutParams(iconSize, iconSize))
            }, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
        }

        addView(ImageView(context).apply {
            setImageResource(R.drawable.ic_ios_delete)
            setColorFilter(textColor)
            scaleType = ImageView.ScaleType.CENTER
            contentDescription = context.getString(R.string.spoken_description_delete)
            setOnTouchListener { v, event -> onDeleteTouch(v, event) }
        }, LayoutParams((44 * dp).toInt(), LayoutParams.MATCH_PARENT))
        setCurrentSection(0)
    }

    fun setCurrentSection(index: Int) {
        icons.forEachIndexed { i, icon ->
            val current = i == index
            icon.setColorFilter(withAlpha(textColor, if (current) 0xCC else 0x80), PorterDuff.Mode.SRC_IN)
            icon.background = if (!current) null else GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(withAlpha(textColor, 0x26))
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun onDeleteTouch(view: android.view.View, event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                view.alpha = 0.5f
                deleteRepeats = 0
                onDeleteDown()
                postDelayed(repeatDelete, IosEmojiLayout.deleteRepeatDelay(0))
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                view.alpha = 1f
                removeCallbacks(repeatDelete)
                onDeleteUp()
            }
        }
        return true
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(repeatDelete)
        super.onDetachedFromWindow()
    }

    private fun withAlpha(color: Int, alpha: Int) = (color and 0x00FFFFFF) or (alpha shl 24)
}
