// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.emoji

import android.graphics.PorterDuff
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import helium314.keyboard.latin.R
import helium314.keyboard.latin.ios.IosEmojiLayout
import helium314.keyboard.latin.utils.ResourceUtils

/**
 * iOS emoji grid: all sections side by side in one horizontal list, keys in 5-row columns,
 * the title of the section at the left edge sticks there until the next section pushes it out.
 */
internal class IosEmojiPalette(
    host: ViewGroup,
    private val emojiCategory: EmojiCategory,
    private val callback: EmojiViewCallback,
    totalHeight: Int,
    private val titleColor: Int,
    fieldColor: Int,
    private val onSectionChanged: (Int) -> Unit,
    private val onRecentsHidden: () -> Unit,
    onSearch: () -> Unit,
) {
    private val res = host.resources
    private val dp = res.displayMetrics.density * ResourceUtils.iosWidthScale(res.displayMetrics)
    private val topPadding = (6 * dp).toInt()
    private val titleHeight = (20 * dp).toInt()
    val bottomRowHeight = (40 * dp).toInt()
    private val gridHeight = totalHeight - searchBlockHeight(res) - bottomRowHeight
    val rowStep = (gridHeight - topPadding - titleHeight) / IosEmojiLayout.ROWS
    val columnStep = (47 * dp).toInt()
    private val sectionGap = (14 * dp).toInt()
    val textSize = minOf(columnStep * 0.64f, rowStep * 0.85f)

    /** the "Search Emoji" pill on top; tapping it opens the search above the letters */
    val searchField: View = LinearLayout(host.context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val fieldHeight = (36 * dp).toInt()
        background = GradientDrawable().apply {
            cornerRadius = fieldHeight / 2f
            setColor(fieldColor)
        }
        setPadding((10 * dp).toInt(), 0, (10 * dp).toInt(), 0)
        val hintColor = titleColor
        addView(ImageView(host.context).apply {
            setImageResource(R.drawable.ic_ios_search)
            setColorFilter(hintColor, PorterDuff.Mode.SRC_IN)
        }, LinearLayout.LayoutParams((18 * dp).toInt(), (18 * dp).toInt()).apply { marginEnd = (6 * dp).toInt() })
        addView(TextView(host.context).apply {
            setText(R.string.ios_emoji_search_hint)
            setTextColor(hintColor)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, 17 * dp)
            isSingleLine = true
        })
        setOnClickListener { onSearch() }
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, fieldHeight).apply {
            val side = (8 * dp).toInt()
            setMargins(side, (8 * dp).toInt(), side, 0)
        }
    }

    var sections: List<IosEmojiLayout.Section> = emptyList()
        private set
    private var currentSection = -1

    val recyclerView = RecyclerView(host.context).apply {
        layoutManager = LinearLayoutManager(host.context, LinearLayoutManager.HORIZONTAL, false)
        setPadding((5 * dp).toInt(), topPadding, (5 * dp).toInt(), 0)
        clipToPadding = false
        overScrollMode = View.OVER_SCROLL_NEVER
        adapter = SectionAdapter()
        addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) = updateTitlesAndSection()
        })
        addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> updateTitlesAndSection() }
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, gridHeight)
    }

    init {
        emojiCategory.iosCell = columnStep to rowStep
    }

    /** rebuilds the list (recents may have become non-empty) and goes back to the first section */
    fun start() {
        val recentsEmpty = emojiCategory.getKeyboard(EmojiCategory.Category.RECENTS, 0).sortedKeys.isEmpty()
        sections = emojiCategory.iosSections.filter { !(recentsEmpty && it.categories.first() == EmojiCategory.Category.RECENTS) }
        @Suppress("NotifyDataSetChanged")
        recyclerView.adapter!!.notifyDataSetChanged()
        recyclerView.scrollToPosition(0)
        currentSection = -1
        setCurrent(0)
    }

    fun scrollToSection(index: Int) {
        (recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(index, if (index > 0) -sectionGap else 0)
        setCurrent(index)
    }

    fun notifySectionChanged(category: EmojiCategory.Category) {
        val i = sections.indexOfFirst { category in it.categories }
        if (i < 0) return
        // may be called from a detach callback in the middle of a scroll, when the adapter must not be notified yet
        recyclerView.post { recyclerView.adapter!!.notifyItemChanged(i) }
    }

    private fun setCurrent(index: Int) {
        if (index == currentSection || index !in sections.indices) return
        currentSection = index
        emojiCategory.currentCategory = sections[index].categories.first()
        onSectionChanged(index)
    }

    private fun updateTitlesAndSection() {
        val lm = recyclerView.layoutManager as LinearLayoutManager
        val count = recyclerView.childCount
        if (count == 0) return
        val lefts = IntArray(count)
        val rights = IntArray(count)
        for (i in 0 until count) {
            val child = recyclerView.getChildAt(i)
            val title = child.findViewById<View>(R.id.ios_emoji_section_title)
            title.translationX = IosEmojiLayout.stickyTitleX(child.left - recyclerView.paddingLeft, child.width, title.width).toFloat()
            lefts[i] = child.left
            rights[i] = child.right
        }
        val visible = IosEmojiLayout.currentSection(lefts, rights, recyclerView.paddingLeft + columnStep / 2)
        setCurrent(lm.getPosition(recyclerView.getChildAt(visible)))
    }

    companion object {
        /** height of the search pill on top including its margin, added to the emoji screen */
        fun searchBlockHeight(res: android.content.res.Resources) =
            ((36 + 8) * res.displayMetrics.density * ResourceUtils.iosWidthScale(res.displayMetrics)).toInt()
    }

    private inner class SectionAdapter : RecyclerView.Adapter<SectionAdapter.Holder>() {
        inner class Holder(view: LinearLayout, val title: TextView, val keyboardView: EmojiPageKeyboardView) : RecyclerView.ViewHolder(view) {
            var isRecents = false
        }

        override fun getItemCount() = sections.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val context = parent.context
            val title = TextView(context).apply {
                id = R.id.ios_emoji_section_title
                setTextColor(titleColor)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, 12 * dp)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isSingleLine = true
                setPadding(((columnStep - textSize) / 2).toInt().coerceAtLeast(0), 0, 0, 0)
            }
            val keyboardView = LayoutInflater.from(context).inflate(R.layout.emoji_keyboard_page, parent, false) as EmojiPageKeyboardView
            keyboardView.setEmojiViewCallback(callback)
            keyboardView.setLabelTextSizeOverride(textSize)
            val item = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                addView(title, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, titleHeight))
                addView(keyboardView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)
            }
            return Holder(item, title, keyboardView)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val section = sections[position]
            holder.isRecents = section.categories.first() == EmojiCategory.Category.RECENTS
            holder.title.text = holder.title.context.getString(section.title).uppercase(res.configuration.locales[0])
            holder.title.translationX = 0f
            holder.keyboardView.setKeyboard(emojiCategory.getIosSectionKeyboard(section))
            (holder.itemView.layoutParams as RecyclerView.LayoutParams).marginStart = if (position > 0) sectionGap else 0
        }

        override fun onViewDetachedFromWindow(holder: Holder) {
            holder.keyboardView.releaseCurrentKey(false)
            holder.keyboardView.deallocateMemory()
            if (holder.isRecents) onRecentsHidden()
        }
    }
}
