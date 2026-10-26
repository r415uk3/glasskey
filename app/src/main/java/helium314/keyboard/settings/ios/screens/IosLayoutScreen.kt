// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.ios.screens

import android.content.Context
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
import helium314.keyboard.latin.common.StringUtils
import helium314.keyboard.latin.settings.SettingsSubtype
import helium314.keyboard.latin.settings.SettingsSubtype.Companion.toSettingsSubtype
import helium314.keyboard.latin.utils.LayoutType
import helium314.keyboard.latin.utils.LayoutUtils
import helium314.keyboard.latin.utils.LayoutUtilsCustom
import helium314.keyboard.latin.utils.SubtypeLocaleUtils
import helium314.keyboard.latin.utils.SubtypeLocaleUtils.displayName
import helium314.keyboard.latin.utils.SubtypeSettings
import helium314.keyboard.latin.utils.SubtypeUtilsAdditional
import helium314.keyboard.latin.utils.prefs
import helium314.keyboard.settings.ios.IosCheckRow
import helium314.keyboard.settings.ios.IosScreen
import helium314.keyboard.settings.ios.iosGroup
import helium314.keyboard.settings.screens.withMainLayout
import helium314.keyboard.settings.screens.withMatchingNoShiftKey
import java.util.Locale

/**
 * Main layout choice for [subtypePref]. For an enabled keyboard the layout is changed in place; otherwise
 * picking a layout enables the keyboard (appended at the end) and calls [onAdded].
 */
@Composable
fun IosLayoutScreen(subtypePref: String, onBack: () -> Unit, onAdded: () -> Unit) {
    val ctx = LocalContext.current
    var current by rememberSaveable { mutableStateOf(subtypePref) }
    val subtype = current.toSettingsSubtype()
    val adding by rememberSaveable { mutableStateOf(SubtypeSettings.getEnabledSubtypes().none { it.toSettingsSubtype() == subtypePref.toSettingsSubtype() }) }
    val layouts = remember(subtype.locale) {
        (LayoutUtils.getAvailableLayouts(LayoutType.MAIN, ctx, subtype.locale) +
            LayoutUtilsCustom.getLayoutFiles(LayoutType.MAIN, ctx, subtype.locale).map { it.name })
            .distinct().map { it to iosLayoutLabel(ctx, it, subtype.locale) }
    }
    val selected = subtype.mainLayoutName() ?: SubtypeLocaleUtils.QWERTY
    val header = stringResource(R.string.ios_layout)
    val footer = if (adding) stringResource(R.string.ios_choose_layout) else null

    IosScreen(SubtypeLocaleUtils.getSubtypeLocaleDisplayName(subtype.locale), onBack) {
        iosGroup(header = header, footer = footer, rows = layouts.map { (layout, label) ->
            {
                IosCheckRow(label, checked = !adding && layout == selected) {
                    val new = subtype.withMainLayout(layout).withMatchingNoShiftKey(ctx)
                    if (adding) {
                        enableSubtype(ctx, new)
                        onAdded()
                    } else if (new != subtype) {
                        SubtypeUtilsAdditional.changeAdditionalSubtype(subtype, new, ctx)
                        current = new.toPref()
                    }
                }
            }
        })
    }
}

/** Enables [subtype] at the end of the user's order, storing it as an additional subtype unless it is a built-in one. */
private fun enableSubtype(context: Context, subtype: SettingsSubtype) {
    val resource = SubtypeSettings.getResourceSubtypesForLocale(subtype.locale).firstOrNull { it.toSettingsSubtype() == subtype }
    if (resource != null) {
        SubtypeSettings.addEnabledSubtype(context.prefs(), resource)
        return
    }
    SubtypeUtilsAdditional.changeAdditionalSubtype(subtype, subtype, context)
    SubtypeSettings.addEnabledSubtype(context.prefs(), subtype.toAdditionalSubtype())
}

/**
 * Short layout name shown in grey: the predefined/custom name (QWERTY), the variant of a language-specific
 * layout («Русский (Расширенная)» → «Расширенная»), or the first keys of the language's own layout (ЙЦУКЕН).
 */
internal fun iosLayoutLabel(context: Context, layout: String, locale: Locale): String {
    SubtypeLocaleUtils.getMainLayoutDisplayName(layout)?.let { return it }
    val name = SubtypeLocaleUtils.getLayoutDisplayNameInSystemLocale(layout, locale)
    // the plain language name, as the shortest name of the language's built-in subtypes
    val language = SubtypeSettings.getResourceSubtypesForLocale(locale).map { it.displayName() }.minByOrNull { it.length }
    if (language == null || name == language) return firstKeys(context, layout, locale) ?: name
    val rest = name.removePrefix(language).trim()
    if (rest != name && rest.startsWith("(") && rest.endsWith(")"))
        return StringUtils.capitalizeFirstCodePoint(rest.removeSurrounding("(", ")"), context.resources.configuration.locale())
    return name
}

private fun firstKeys(context: Context, layout: String, locale: Locale): String? = runCatching {
    val content = LayoutUtils.getContent(LayoutType.MAIN, layout, context)
    if (content.trimStart().startsWith("[")) return null // json layout
    val keys = LayoutUtils.getSimpleRowStrings(content).first().lines()
        .mapNotNull { line -> line.trim().substringBefore(' ').takeIf { it.isNotEmpty() } }
    if (keys.size < 6) null else keys.take(6).joinToString("").uppercase(locale)
}.getOrNull()
