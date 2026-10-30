// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.ios.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import helium314.keyboard.compat.locale
import helium314.keyboard.latin.R
import helium314.keyboard.latin.common.FileUtils
import helium314.keyboard.latin.common.LocaleUtils.constructLocale
import helium314.keyboard.latin.dictionary.Dictionary
import helium314.keyboard.latin.dictionary.DictionaryInfoUtils
import helium314.keyboard.latin.dictionary.getKnownDictionariesForLocale
import helium314.keyboard.latin.makedict.DictionaryHeader
import helium314.keyboard.latin.utils.IntentUtils
import helium314.keyboard.latin.utils.ScriptUtils.script
import helium314.keyboard.dictionarypack.DictionaryPackConstants
import helium314.keyboard.settings.dialogs.addedDictionaryFile
import helium314.keyboard.settings.dialogs.checkDict
import helium314.keyboard.settings.dialogs.dictionaryLocaleChoices
import helium314.keyboard.settings.dialogs.installDictionary
import helium314.keyboard.settings.ios.IosActionSheet
import helium314.keyboard.settings.ios.IosCheckRow
import helium314.keyboard.settings.ios.IosDestructive
import helium314.keyboard.settings.ios.IosGroupShape
import helium314.keyboard.settings.ios.IosRoute
import helium314.keyboard.settings.ios.IosRow
import helium314.keyboard.settings.ios.IosScreen
import helium314.keyboard.settings.ios.IosSeparator
import helium314.keyboard.settings.ios.IosSheetCard
import helium314.keyboard.settings.ios.LocalIosColors
import helium314.keyboard.settings.ios.iosGroup
import helium314.keyboard.settings.ios.iosPanel
import helium314.keyboard.settings.ios.openLink
import helium314.keyboard.settings.screens.getUserAndInternalDictionaries
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/**
 * One dictionary of a language: the built-in one (from the app, [replaced] when an added main dictionary is used
 * instead) or an added file, with what its header says.
 */
data class IosDictionaryInfo(
    val type: String,
    val builtIn: Boolean,
    val version: String?,
    val date: String?,
    val description: String?,
    val replaced: Boolean = false,
)

/** Where the dictionary pages read and delete dictionaries. */
interface IosDictionaryStore {
    /** The built-in dictionary first (if there is one), then the added ones by type. */
    fun dictionaries(context: Context, locale: Locale): List<IosDictionaryInfo>
    /** Deletes the added dictionary of [type]. */
    fun delete(context: Context, locale: Locale, type: String)
}

object IosDictionaryFiles : IosDictionaryStore {
    override fun dictionaries(context: Context, locale: Locale): List<IosDictionaryInfo> {
        val (files, hasInternal) = getUserAndInternalDictionaries(context, locale)
        val uiLocale = context.resources.configuration.locale()
        val added = files.mapNotNull { file ->
            val header = DictionaryInfoUtils.getDictionaryFileHeaderOrNull(file) ?: return@mapNotNull null
            IosDictionaryInfo(header.type(), false, header.mVersionString, header.date(uiLocale), header.description)
        }.sortedWith(compareBy({ it.type != Dictionary.TYPE_MAIN }, { it.type }))
        if (!hasInternal) return added
        val builtIn = IosDictionaryInfo(Dictionary.TYPE_MAIN, true, null, null, null, replaced = added.any { it.type == Dictionary.TYPE_MAIN })
        return listOf(builtIn) + added
    }

    override fun delete(context: Context, locale: Locale, type: String) {
        addedDictionaryFile(context, locale, type).delete()
        context.sendBroadcast(Intent(DictionaryPackConstants.NEW_DICTIONARY_INTENT_ACTION))
    }

    private fun DictionaryHeader.date(locale: Locale): String? {
        val seconds = mDictionaryOptions.mAttributes[DictionaryHeader.DICTIONARY_DATE_KEY]?.toLongOrNull() ?: return null
        return DateFormat.getDateInstance(DateFormat.SHORT, locale).format(Date(seconds * 1000L))
    }
}

private fun DictionaryHeader.type() = mIdString.substringBefore(":")

@Composable
private fun IosDictionaryInfo.title() = if (type == Dictionary.TYPE_MAIN) stringResource(R.string.main_dictionary) else type

/** Re-reads [read] when the page comes back (e.g. from the file picker) and whenever [refresh] changes. */
@Composable
private fun <T> rememberReloading(refresh: Int, read: () -> T): T {
    var resumed by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { resumed++ }
    return remember(refresh, resumed) { read() }
}

/**
 * A language's page in «Словари»: its built-in and added dictionaries, adding a file, and the dictionaries that
 * can be downloaded for it.
 */
@Composable
fun IosDictionaryLanguageScreen(
    locale: Locale,
    onBack: () -> Unit,
    navigate: (String) -> Unit,
    store: IosDictionaryStore = IosDictionaryFiles,
) {
    val ctx = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val tint = LocalIosColors.current.tint
    var added by remember { mutableIntStateOf(0) }
    val dictionaries = rememberReloading(added) { store.dictionaries(ctx, locale) }
    val downloads = remember(locale) { runCatching { getKnownDictionariesForLocale(locale, ctx) }.getOrDefault(emptyList()) }
    val picker = iosDictionaryFilePicker(locale) { added++ }

    val builtIn = stringResource(R.string.ios_dictionary_builtin)
    val addedCaption = stringResource(R.string.ios_dictionary_added)
    val replacedFooter = stringResource(R.string.ios_dictionary_replaced_footer)
    val header = stringResource(R.string.ios_dictionaries_header)
    val addDictionary = stringResource(R.string.ios_add_dictionary)
    val downloadHeader = stringResource(R.string.ios_dictionary_download_header)
    val titles = dictionaries.map { it.title() }

    IosScreen(dictionaryLanguageName(locale, ctx), onBack) {
        if (dictionaries.isNotEmpty())
            iosGroup(
                header = header,
                footer = if (dictionaries.any { it.replaced }) replacedFooter else null,
                rows = dictionaries.mapIndexed { i, d ->
                    { ->
                        IosRow(
                            titles[i],
                            value = d.version?.let { "v $it" },
                            subtitle = if (d.builtIn) builtIn else addedCaption,
                            titleColor = if (d.replaced) LocalIosColors.current.secondaryText else null,
                            onClick = if (d.builtIn) null else { -> navigate(IosRoute.DictionaryFile + locale.toLanguageTag() + "/" + d.type) },
                        )
                    }
                },
            )
        iosGroup(rows = listOf({ -> IosRow(addDictionary, titleColor = tint, chevron = false, onClick = { picker.launchDictionaryPicker(ctx) }) }))
        if (downloads.isNotEmpty())
            iosGroup(header = downloadHeader, rows = downloads.map { (name, link) ->
                { -> IosRow(name, titleColor = tint, chevron = false, onClick = { uriHandler.openLink(link) }) }
            })
    }
}

/** An added dictionary's page: what its header says, and deleting it (after an action sheet). */
@Composable
fun IosDictionaryFileScreen(
    locale: Locale,
    type: String,
    onBack: () -> Unit,
    store: IosDictionaryStore = IosDictionaryFiles,
) {
    val ctx = LocalContext.current
    val dictionary = remember(locale, type) { store.dictionaries(ctx, locale).firstOrNull { !it.builtIn && it.type == type } }
    var confirming by remember { mutableStateOf(false) }
    val title = dictionary?.title() ?: type
    val language = stringResource(R.string.ios_dictionary_language)
    val version = stringResource(R.string.ios_dictionary_version)
    val date = stringResource(R.string.ios_dictionary_date)
    val delete = stringResource(R.string.ios_delete_dictionary)
    val languageName = dictionaryLanguageName(locale, ctx)

    IosScreen(title, onBack) {
        if (dictionary == null) return@IosScreen
        val rows = mutableListOf<@Composable () -> Unit>({ -> IosRow(language, value = languageName) })
        dictionary.version?.let { rows += { -> IosRow(version, value = it) } }
        dictionary.date?.let { rows += { -> IosRow(date, value = it) } }
        iosGroup(footer = dictionary.description?.takeIf { it.isNotBlank() }, rows = rows)
        iosGroup(rows = listOf({ -> IosRow(delete, titleColor = IosDestructive, chevron = false, onClick = { confirming = true }) }))
    }

    if (confirming)
        IosActionSheet(
            stringResource(R.string.ios_delete_dictionary_message, title),
            action = delete,
            onAction = { store.delete(ctx, locale, type) },
            onDismiss = { confirming = false; if (store.dictionaries(ctx, locale).none { !it.builtIn && it.type == type }) onBack() },
        )
}

/** iOS system orange, for warnings. */
private val IosWarning = androidx.compose.ui.graphics.Color(0xFFFF9F0A)

/** A checked dictionary file waiting to be added: what it is, and the languages it can be added to. */
data class IosDictionaryCandidate(
    val type: String,
    val version: String,
    val dictLocale: Locale,
    val locales: List<Locale>,
    val initial: Locale,
)

/**
 * The card for adding a picked dictionary file: its name and version, the language to add it to (a tap on the row
 * lists the choices), and a warning when it replaces a dictionary ([existing]) or the alphabets differ.
 */
@Composable
fun IosNewDictionarySheet(
    candidate: IosDictionaryCandidate,
    existing: (Locale) -> IosDictionaryInfo?,
    onAdd: (Locale) -> Unit,
    onDismiss: () -> Unit,
) {
    val ctx = LocalContext.current
    val colors = LocalIosColors.current
    var locale by remember { mutableStateOf(candidate.initial) }
    var choosing by remember { mutableStateOf(false) }
    val replaces = existing(locale)
    IosSheetCard(
        stringResource(R.string.ios_new_dictionary),
        stringResource(if (replaces != null) R.string.ios_replace else R.string.ios_add),
        canConfirm = true,
        onConfirm = { onAdd(locale) },
        onDismiss = onDismiss,
    ) {
        Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth().iosPanel(IosGroupShape)) {
            IosRow(stringResource(R.string.ios_dictionary_name), value = candidate.type)
            IosSeparator()
            IosRow(stringResource(R.string.ios_dictionary_version), value = candidate.version)
            IosSeparator()
            IosRow(
                stringResource(R.string.ios_dictionary_language),
                value = dictionaryLanguageName(locale, ctx),
                chevron = false,
                onClick = { choosing = !choosing },
            )
            if (choosing)
                Column(Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState())) {
                    candidate.locales.forEach { l ->
                        IosSeparator()
                        IosCheckRow(dictionaryLanguageName(l, ctx), l == locale) { locale = l; choosing = false }
                    }
                }
        }
        val warnings = listOfNotNull(
            replaces?.let { stringResource(R.string.ios_new_dictionary_replaces, it.type, it.version ?: "?") },
            if (locale.script() != candidate.dictLocale.script()) stringResource(R.string.ios_dictionary_wrong_script) else null,
        )
        warnings.forEach {
            BasicText(
                it,
                Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp),
                style = TextStyle(color = IosWarning, fontSize = 13.sp, lineHeight = 17.sp),
            )
        }
    }
}

private fun ManagedActivityResultLauncher<Intent, ActivityResult>.launchDictionaryPicker(context: Context) =
    launch(IntentUtils.getResolvableTypeIntent(context, "*/*").setAction(Intent.ACTION_OPEN_DOCUMENT))

/**
 * Picks a dictionary file, checks it and shows [IosNewDictionarySheet] (or a notice when the file is no dictionary);
 * [onAdded] after it was added. [mainLocale] is the language to suggest, null to guess from the file.
 */
@Composable
fun iosDictionaryFilePicker(mainLocale: Locale?, onAdded: () -> Unit): ManagedActivityResultLauncher<Intent, ActivityResult> {
    val ctx = LocalContext.current
    val cachedFile = remember { File(ctx.cacheDir, "temp_dict") }
    var candidate by remember { mutableStateOf<Pair<DictionaryHeader, IosDictionaryCandidate>?>(null) }
    var error by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data
        if (result.resultCode != Activity.RESULT_OK || uri == null) return@rememberLauncherForActivityResult
        cachedFile.delete()
        FileUtils.copyContentUriToNewFile(uri, ctx, cachedFile)
        val (failed, header) = checkDict(cachedFile)
        if (failed != null || header == null) { cachedFile.delete(); error = true; return@rememberLauncherForActivityResult }
        val dictLocale = header.mLocaleString.constructLocale()
        val (locales, initial) = dictionaryLocaleChoices(dictLocale, mainLocale)
        candidate = header to IosDictionaryCandidate(header.type(), header.mVersionString, dictLocale, locales, initial)
    }
    candidate?.let { (header, c) ->
        IosNewDictionarySheet(
            c,
            existing = { l -> IosDictionaryFiles.dictionaries(ctx, l).firstOrNull { !it.builtIn && it.type == c.type } },
            onAdd = { l -> installDictionary(ctx, cachedFile, header, l); onAdded() },
            onDismiss = { candidate = null; cachedFile.delete() },
        )
    }
    if (error)
        IosActionSheet(
            stringResource(R.string.ios_dictionary_file_error),
            action = null,
            onAction = {},
            onDismiss = { error = false },
            cancel = stringResource(R.string.ios_ok),
        )
    return picker
}
