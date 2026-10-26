// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.ios

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import helium314.keyboard.keyboard.emoji.EmojiCategory.Category
import helium314.keyboard.latin.R

/** Geometry and order of the iOS emoji screen: one horizontal strip of sections, keys fill columns top to bottom. */
internal object IosEmojiLayout {
    fun rows(landscape: Boolean) = if (landscape) 3 else 5

    data class Cell(val column: Int, val row: Int)

    class Section(val categories: List<Category>, @StringRes val title: Int, @DrawableRes val icon: Int)

    fun cell(index: Int, rows: Int) = Cell(index / rows, index % rows)

    fun columnCount(keyCount: Int, rows: Int) = (keyCount + rows - 1) / rows

    /** x of a section title inside its section: it stays at the screen edge until the section's end pushes it out */
    fun stickyTitleX(itemLeft: Int, itemWidth: Int, titleWidth: Int) =
        (-itemLeft).coerceIn(0, maxOf(0, itemWidth - titleWidth))

    /** index of the section under [anchorX]; in the gap between two sections the next one counts */
    fun currentSection(lefts: IntArray, rights: IntArray, anchorX: Int): Int {
        for (i in lefts.indices) {
            if (anchorX < rights[i]) return i
        }
        return lefts.lastIndex
    }

    fun sections(withFlags: Boolean) = listOfNotNull(
        Section(listOf(Category.RECENTS), R.string.ios_emoji_section_recents, R.drawable.ic_emoji_recents_rounded),
        Section(listOf(Category.SMILEYS, Category.PEOPLE), R.string.ios_emoji_section_smileys, R.drawable.ic_emoji_smileys_emotion_rounded),
        Section(listOf(Category.NATURE), R.string.ios_emoji_section_nature, R.drawable.ic_emoji_animals_nature_lxx),
        Section(listOf(Category.FOOD), R.string.ios_emoji_section_food, R.drawable.ic_emoji_food_drink_rounded),
        Section(listOf(Category.ACTIVITIES), R.string.ios_emoji_section_activity, R.drawable.ic_emoji_activities_rounded),
        Section(listOf(Category.TRAVEL_PLACES), R.string.ios_emoji_section_travel, R.drawable.ic_emoji_travel_places_rounded),
        Section(listOf(Category.OBJECTS), R.string.ios_emoji_section_objects, R.drawable.ic_emoji_objects_rounded),
        Section(listOf(Category.SYMBOLS), R.string.ios_emoji_section_symbols, R.drawable.ic_emoji_symbols_rounded),
        if (withFlags) Section(listOf(Category.FLAGS), R.string.ios_emoji_section_flags, R.drawable.ic_emoji_flags_rounded) else null,
    )

    fun deleteRepeatDelay(repeatsSoFar: Int) = if (repeatsSoFar == 0) 400L else 50L
}
