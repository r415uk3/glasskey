// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.ios.screens

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import helium314.keyboard.latin.R
import helium314.keyboard.latin.common.Links
import helium314.keyboard.latin.common.LocaleUtils.localizedDisplayName
import helium314.keyboard.latin.common.splitOnWhitespace
import helium314.keyboard.latin.dictionary.Dictionary
import helium314.keyboard.latin.dictionary.DictionaryInfoUtils
import helium314.keyboard.latin.dictionary.getDictionaryLocales
import helium314.keyboard.latin.utils.IntentUtils
import helium314.keyboard.latin.utils.SubtypeSettings
import helium314.keyboard.latin.utils.locale
import helium314.keyboard.settings.ios.openLink
import helium314.keyboard.settings.ios.IosRow
import helium314.keyboard.settings.ios.IosScreen
import helium314.keyboard.settings.ios.IosSearchField
import helium314.keyboard.settings.ios.LocalIosColors
import helium314.keyboard.settings.ios.iosGroup
import helium314.keyboard.settings.screens.getUserAndInternalDictionaries
import java.util.Locale

/** A language that has dictionaries, with its name and a caption listing them. */
data class IosDictionaryLanguage(val locale: Locale, val name: String, val dictionaries: String)

/** Languages with built-in or added dictionaries: enabled languages first, then those with added ones, by name. */
fun iosDictionaryLanguages(context: Context): List<IosDictionaryLanguage> {
    val enabledLanguages = SubtypeSettings.getEnabledSubtypes(true).map { it.locale().language }
    val cachedDictFolders = DictionaryInfoUtils.getCacheDirectories(context).map { it.name }
    val comparer = compareBy<Locale>({ it.language !in enabledLanguages }, { it.toLanguageTag() !in cachedDictFolders }, { it.displayName })
    val internal = context.getString(R.string.ios_internal_dictionary)
    return getDictionaryLocales(context).sortedWith(comparer).map { locale ->
        val (dicts, hasInternal) = getUserAndInternalDictionaries(context, locale)
        val types = dicts.mapTo(mutableListOf()) { it.name.substringBefore("_${DictionaryInfoUtils.USER_DICTIONARY_SUFFIX}") }
        if (hasInternal && !types.contains(Dictionary.TYPE_MAIN)) types.add(0, internal)
        IosDictionaryLanguage(locale, dictionaryLanguageName(locale, context), types.joinToString(", "))
    }
}

/** The language's name as a title: Russian names start lower-case («казахский»). */
fun dictionaryLanguageName(locale: Locale, context: Context): String {
    val uiLocale = context.resources.configuration.locales[0]
    return locale.localizedDisplayName(context.resources).replaceFirstChar { it.titlecase(uiLocale) }
}

/** Same matching as HeliBoard's dictionary screen: any word of the name starts with the term. */
private fun IosDictionaryLanguage.matches(term: String) =
    name.replace("(", "").splitOnWhitespace().any { it.startsWith(term, ignoreCase = true) }

/**
 * Dictionaries per language; «Добавить словарь…» opens the file picker. A language row opens HeliBoard's
 * dictionary details dialog (add / delete per language), unless [onOpenLanguage] is given.
 */
@Composable
fun IosDictionariesScreen(
    onBack: () -> Unit,
    loadLanguages: (Context) -> List<IosDictionaryLanguage> = ::iosDictionaryLanguages,
    onOpenLanguage: (Locale) -> Unit,
) {
    val ctx = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val tint = LocalIosColors.current.tint
    var query by rememberSaveable { mutableStateOf("") }
    var added by remember { mutableIntStateOf(0) }
    val picker = iosDictionaryFilePicker(null) { added++ }
    // re-read on every composition (as HeliBoard does), so dictionaries added or deleted on the language pages show up
    val languages = loadLanguages(ctx)
    val term = query.trim()
    val shown = if (term.isEmpty()) languages else languages.filter { it.matches(term) }

    val addDictionary = stringResource(R.string.ios_add_dictionary)
    val getDictionaries = stringResource(R.string.ios_get_dictionaries)
    val addFooter = stringResource(R.string.ios_add_dictionary_footer)
    val header = stringResource(R.string.ios_languages)
    val noResults = stringResource(R.string.ios_no_results)

    IosScreen(
        stringResource(R.string.ios_dictionaries),
        onBack,
        bottomBar = { IosSearchField(query, { query = it }) },
    ) {
        if (term.isEmpty())
            iosGroup(footer = addFooter, rows = listOf(
                { ->
                    IosRow(addDictionary, titleColor = tint, chevron = false, onClick = {
                        picker.launch(IntentUtils.getResolvableTypeIntent(ctx, "*/*").setAction(Intent.ACTION_OPEN_DOCUMENT))
                    })
                },
                { -> IosRow(getDictionaries, titleColor = tint, chevron = false, onClick = { uriHandler.openLink(Links.DICTIONARY_URL) }) },
            ))
        if (shown.isEmpty())
            iosGroup(footer = noResults, rows = emptyList())
        else
            iosGroup(header = header, rows = shown.map { language ->
                { ->
                    IosRow(language.name, subtitle = language.dictionaries.ifEmpty { null }, onClick = {
                        onOpenLanguage(language.locale)
                    })
                }
            })
    }
}
