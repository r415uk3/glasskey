// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.ios

import android.content.Context
import java.util.Locale

/**
 * Emoji search over CLDR names and keywords of all three keyboard languages at once.
 * Every query word must be the start of a word of the emoji's name or keywords; name matches come first.
 */
class IosEmojiSearchIndex private constructor(private val entries: List<Entry>) {
    private class Entry(val emoji: String, val nameWords: MutableSet<String>, val keywordWords: MutableSet<String>)

    fun search(query: String): List<String> {
        val tokens = words(query)
        if (tokens.isEmpty()) return emptyList()
        val byName = ArrayList<String>()
        val byKeyword = ArrayList<String>()
        for (entry in entries) {
            if (tokens.all { t -> entry.nameWords.any { it.startsWith(t) } }) byName.add(entry.emoji)
            else if (tokens.all { t -> entry.nameWords.any { it.startsWith(t) } || entry.keywordWords.any { it.startsWith(t) } })
                byKeyword.add(entry.emoji)
        }
        return byName + byKeyword
    }

    companion object {
        val LANGUAGES = listOf("en", "ru", "kk")

        /** each list holds the lines of one language file: emoji TAB name TAB keyword|keyword... */
        fun of(vararg languages: List<String>): IosEmojiSearchIndex {
            val entries = LinkedHashMap<String, Entry>()
            for (lines in languages) {
                for (line in lines) {
                    val parts = line.split('\t')
                    if (parts.size < 2) continue
                    val entry = entries.getOrPut(parts[0]) { Entry(parts[0], HashSet(), HashSet()) }
                    entry.nameWords.addAll(words(parts[1]))
                    if (parts.size > 2) entry.keywordWords.addAll(words(parts[2]))
                }
            }
            return IosEmojiSearchIndex(entries.values.toList())
        }

        fun load(context: Context) = of(*LANGUAGES.map { lang ->
            context.assets.open("emoji_search/$lang.txt").bufferedReader().use { it.readLines() }
        }.toTypedArray())

        private fun words(text: String) = text.lowercase(Locale.ROOT).replace('ё', 'е')
            .split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotEmpty() }
    }
}
