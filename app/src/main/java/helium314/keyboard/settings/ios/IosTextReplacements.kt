// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.ios

import android.content.Context
import android.provider.UserDictionary
import helium314.keyboard.settings.screens.WEIGHT_FOR_USER_DICTIONARY_ADDS
import helium314.keyboard.settings.screens.deleteWord
import helium314.keyboard.settings.screens.getAll
import helium314.keyboard.settings.screens.getSortedDictionaryLocales
import java.util.Locale

/** A personal dictionary entry with a shortcut; [locale] null means all languages. */
data class TextReplacement(val word: String, val shortcut: String?, val locale: Locale?)

/** iOS "Замена текста": the shortcut entries of the personal dictionary. */
object IosTextReplacements {
    // "all languages" plus the locales of the enabled languages, as the personal dictionary screens see them
    private fun locales(): List<Locale?> = listOf<Locale?>(null) + getSortedDictionaryLocales()

    fun all(context: Context): List<TextReplacement> = locales().flatMap { locale ->
        getAll(locale, context).filter { !it.shortcut.isNullOrBlank() }.map { TextReplacement(it.word, it.shortcut, locale) }
    }

    fun add(context: Context, word: String, shortcut: String) =
        UserDictionary.Words.addWord(context, word, WEIGHT_FOR_USER_DICTIONARY_ADDS, shortcut, null)

    fun delete(context: Context, r: TextReplacement) {
        getAll(r.locale, context).filter { it.word == r.word && it.shortcut == r.shortcut }
            .forEach { deleteWord(it, r.locale, context.contentResolver) }
    }
}
