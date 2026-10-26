// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.ios.screens

import android.content.Context
import android.net.Uri
import android.view.inputmethod.InputMethodSubtype
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import helium314.keyboard.compat.locale
import helium314.keyboard.latin.R
import helium314.keyboard.latin.common.LocaleUtils.localizedDisplayName
import helium314.keyboard.latin.common.StringUtils
import helium314.keyboard.latin.settings.SettingsSubtype.Companion.toSettingsSubtype
import helium314.keyboard.latin.utils.SubtypeLocaleUtils
import helium314.keyboard.latin.utils.SubtypeSettings
import helium314.keyboard.latin.utils.locale
import helium314.keyboard.settings.ios.IosRoute
import helium314.keyboard.settings.ios.IosRow
import helium314.keyboard.settings.ios.IosScreen
import helium314.keyboard.settings.ios.IosSearchField
import helium314.keyboard.settings.ios.iosGroup
import helium314.keyboard.settings.screens.matchingSystemLocales
import java.text.Collator
import java.util.Locale

/** A language that can be added: [name] in the system language, [nativeName] in the language itself. */
internal class IosLanguage(
    val locale: Locale, val name: String, val nativeName: String, val subtype: InputMethodSubtype, val suggested: Boolean,
)

/** Languages without an enabled keyboard: system languages first, then alphabetically, «no language» last. */
internal fun addableLanguages(context: Context): List<IosLanguage> {
    val enabledLocales = SubtypeSettings.getEnabledSubtypes().mapTo(HashSet()) { it.locale() }
    val byLocale = SubtypeSettings.getAllAvailableSubtypes().groupBy { it.locale() }.filterKeys { it !in enabledLocales }
    val systemLocales = matchingSystemLocales(byLocale.keys)
    val systemLocale = context.resources.configuration.locale()
    val collator = Collator.getInstance(systemLocale)
    return byLocale.map { (locale, subtypes) ->
        IosLanguage(
            locale,
            StringUtils.capitalizeFirstCodePoint(locale.localizedDisplayName(context.resources), systemLocale),
            SubtypeLocaleUtils.getSubtypeLocaleDisplayName(locale),
            SubtypeSettings.getResourceSubtypesForLocale(locale).firstOrNull() ?: subtypes.first(),
            locale in systemLocales,
        )
    }.sortedWith(
        compareBy<IosLanguage> { systemLocales.indexOf(it.locale).let { i -> if (i < 0) Int.MAX_VALUE else i } }
            .thenBy { it.locale.toLanguageTag() == SubtypeLocaleUtils.NO_LANGUAGE }
            .thenComparator { a, b -> collator.compare(a.name, b.name) }
    )
}

private fun IosLanguage.matches(term: String) =
    listOf(name, nativeName).any { n -> n.lowercase().split(' ', '(').any { it.startsWith(term) } }

@Composable
fun IosAddKeyboardScreen(onBack: () -> Unit, navigate: (String) -> Unit) {
    val ctx = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    val languages = remember { addableLanguages(ctx) }
    val term = query.trim().lowercase()
    val shown = if (term.isEmpty()) languages else languages.filter { it.matches(term) }
    val suggestedHeader = stringResource(R.string.ios_suggested_languages)
    val noResults = stringResource(R.string.ios_no_results)

    IosScreen(
        stringResource(R.string.ios_add_keyboard).trimEnd('…', '.'),
        onBack,
        bottomBar = { IosSearchField(query, { query = it }) },
    ) {
        if (shown.isEmpty()) {
            iosGroup(footer = noResults, rows = emptyList())
            return@IosScreen
        }
        // one panel per letter keeps each glass layer small
        val (suggested, others) = shown.partition { it.suggested }
        if (suggested.isNotEmpty()) iosGroup(header = suggestedHeader, rows = suggested.map { languageRow(it, navigate) })
        others.groupBy { it.name.take(1).uppercase() }.forEach { (letter, group) ->
            iosGroup(header = letter, rows = group.map { languageRow(it, navigate) })
        }
    }
}

private fun languageRow(language: IosLanguage, navigate: (String) -> Unit): @Composable () -> Unit = {
    IosRow(
        language.name,
        value = language.nativeName.takeIf { it != language.name },
        onClick = { navigate(IosRoute.Layout + Uri.encode(language.subtype.toSettingsSubtype().toPref())) },
    )
}
