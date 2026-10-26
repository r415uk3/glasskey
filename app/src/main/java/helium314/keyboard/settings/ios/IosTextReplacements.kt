// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.ios

import android.content.Context
import android.provider.UserDictionary.Words
import helium314.keyboard.latin.common.LocaleUtils.constructLocale
import helium314.keyboard.settings.screens.WEIGHT_FOR_USER_DICTIONARY_ADDS
import java.util.Locale

/** A personal dictionary entry with a shortcut; [locale] null means all languages.
 *  [storedLocale] is the provider's raw LOCALE value, used to delete exactly that row. */
data class TextReplacement(
    val word: String,
    val shortcut: String?,
    val locale: Locale?,
    internal val storedLocale: String? = locale?.toString(),
)

/** Where [helium314.keyboard.settings.ios.screens.IosTextReplacementScreen] reads and writes its entries. */
interface TextReplacementStore {
    fun all(context: Context): List<TextReplacement>
    /** [locale] null means all languages. */
    fun add(context: Context, word: String, shortcut: String, locale: Locale?)
    fun delete(context: Context, r: TextReplacement)
}

/** iOS "Замена текста": the shortcut entries of the personal dictionary, of every locale. */
object IosTextReplacements : TextReplacementStore {
    override fun all(context: Context): List<TextReplacement> {
        val cursor = context.contentResolver.query(Words.CONTENT_URI, arrayOf(Words.WORD, Words.SHORTCUT, Words.LOCALE),
            "${Words.SHORTCUT} IS NOT NULL AND ${Words.SHORTCUT} != ''", null, "UPPER(${Words.SHORTCUT})") ?: return emptyList()
        return cursor.use {
            val result = mutableListOf<TextReplacement>()
            while (it.moveToNext()) {
                val stored = it.getString(2)?.takeIf { s -> s.isNotEmpty() }
                result.add(TextReplacement(it.getString(0), it.getString(1), stored?.constructLocale(), stored))
            }
            result
        }
    }

    override fun add(context: Context, word: String, shortcut: String, locale: Locale?) =
        Words.addWord(context, word, WEIGHT_FOR_USER_DICTIONARY_ADDS, shortcut, locale)

    override fun delete(context: Context, r: TextReplacement) {
        val shortcut = r.shortcut ?: return
        if (r.storedLocale == null)
            context.contentResolver.delete(Words.CONTENT_URI,
                "${Words.WORD}=? AND ${Words.SHORTCUT}=? AND (${Words.LOCALE} IS NULL OR ${Words.LOCALE}='')", arrayOf(r.word, shortcut))
        else
            context.contentResolver.delete(Words.CONTENT_URI,
                "${Words.WORD}=? AND ${Words.SHORTCUT}=? AND ${Words.LOCALE}=?", arrayOf(r.word, shortcut, r.storedLocale))
    }
}
