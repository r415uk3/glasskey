// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.ios.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import android.view.WindowManager
import kotlinx.coroutines.android.awaitFrame
import helium314.keyboard.latin.R
import helium314.keyboard.latin.common.LocaleUtils.constructLocale
import helium314.keyboard.latin.utils.SubtypeLocaleUtils
import helium314.keyboard.settings.ios.IosBarButton
import helium314.keyboard.settings.ios.IosGlassButton
import helium314.keyboard.settings.ios.IosGroupShape
import helium314.keyboard.settings.ios.IosRow
import helium314.keyboard.settings.ios.IosScreen
import helium314.keyboard.settings.ios.IosSheetCard
import helium314.keyboard.settings.ios.IosSeparator
import helium314.keyboard.settings.ios.IosTextReplacements
import helium314.keyboard.settings.ios.LocalIosColors
import helium314.keyboard.settings.ios.TextReplacement
import helium314.keyboard.settings.ios.TextReplacementStore
import helium314.keyboard.settings.ios.iosGroup
import helium314.keyboard.settings.ios.iosPanel

/** Phrase / shortcut pairs; «+» adds one for all languages, a tap edits one, «Изменить» deletes. */
@Composable
fun IosTextReplacementScreen(onBack: () -> Unit, store: TextReplacementStore = IosTextReplacements) {
    val ctx = LocalContext.current
    var entries by remember { mutableStateOf(store.all(ctx)) }
    var editing by rememberSaveable { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<TextReplacement?>(null) }
    // the entry being edited, or a blank one with a null shortcut for «+»
    var sheet by rememberSaveable(stateSaver = SheetSaver) { mutableStateOf<TextReplacement?>(null) }
    val footer = stringResource(R.string.ios_text_replacement_footer)

    IosScreen(
        stringResource(R.string.ios_text_replacement),
        onBack,
        action = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (entries.isNotEmpty())
                    IosBarButton(stringResource(if (editing) R.string.ios_done else R.string.ios_edit)) {
                        editing = !editing
                        pendingDelete = null
                    }
                if (!editing) PlusButton { sheet = TextReplacement("", null, null) }
            }
        },
    ) {
        iosGroup(footer = footer, rows = entries.map { r ->
            { ->
                val name = r.word
                IosRow(
                    name,
                    value = if (pendingDelete == r) null else r.shortcut,
                    chevron = false,
                    onClick = if (editing) null else { -> sheet = r },
                    subtitle = r.locale?.let { SubtypeLocaleUtils.getSubtypeLocaleDisplayName(it) },
                    leading = if (editing) { -> IosDeleteToggle(name) { pendingDelete = if (pendingDelete == r) null else r } } else null,
                    trailing = if (editing && pendingDelete == r) { ->
                        IosConfirmDeleteButton {
                            store.delete(ctx, r)
                            entries = store.all(ctx)
                            pendingDelete = null
                            if (entries.isEmpty()) editing = false
                        }
                    } else null,
                )
            }
        })
    }

    sheet?.let { edited ->
        ReplacementSheet(
            edited,
            onDismiss = { sheet = null },
            onSave = { word, shortcut ->
                if (edited.shortcut != null) store.delete(ctx, edited)
                val same = store.all(ctx).any { it.word == word && it.shortcut == shortcut && it.storedLocale == edited.storedLocale }
                if (!same) store.add(ctx, word, shortcut, edited.locale)
                entries = store.all(ctx)
            },
        )
    }
}

// keeps the open sheet across activity recreation (rotation); an empty list means no sheet
private val SheetSaver = listSaver<TextReplacement?, String?>(
    save = { r -> if (r == null) emptyList() else listOf(r.word, r.shortcut, r.storedLocale) },
    restore = { if (it.isEmpty()) null else TextReplacement(it[0]!!, it[1], it[2]?.constructLocale(), it[2]) },
)

@Composable
private fun PlusButton(onClick: () -> Unit) {
    val color = LocalIosColors.current.tint
    val description = stringResource(R.string.ios_add)
    IosGlassButton(onClick, Modifier.semantics { contentDescription = description }, circle = true) {
        Canvas(Modifier.size(18.dp)) {
            val w = 2.4.dp.toPx()
            drawLine(color, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), w, StrokeCap.Round)
            drawLine(color, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), w, StrokeCap.Round)
        }
    }
}

/** The phrase and shortcut fields in an [IosSheetCard]; «Готово» saves them. */
@Composable
private fun ReplacementSheet(initial: TextReplacement, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    // cursor at the end when editing an entry
    var phrase by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue(initial.word, TextRange(initial.word.length))) }
    var shortcut by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(initial.shortcut.orEmpty(), TextRange(initial.shortcut.orEmpty().length)))
    }
    val phraseFocus = remember { FocusRequester() }
    IosSheetCard(
        stringResource(R.string.ios_text_replacement),
        stringResource(R.string.ios_done),
        canConfirm = phrase.text.isNotBlank() && shortcut.text.isNotBlank(),
        onConfirm = { onSave(phrase.text.trim(), shortcut.text.trim()) },
        onDismiss = onDismiss,
        focus = phraseFocus,
    ) {
        Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth().iosPanel(IosGroupShape)) {
            SheetField(stringResource(R.string.ios_phrase), phrase, { phrase = it }, phraseFocus)
            IosSeparator()
            SheetField(stringResource(R.string.ios_shortcut), shortcut, { shortcut = it })
        }
    }
}

@Composable
private fun SheetField(
    label: String,
    value: TextFieldValue,
    onChange: (TextFieldValue) -> Unit,
    focus: FocusRequester = remember { FocusRequester() },
) {
    val colors = LocalIosColors.current
    val keyboard = LocalSoftwareKeyboardController.current
    // the whole row, label included, focuses the field (and brings the keyboard back if it was hidden)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 46.dp)
            .clickable(remember { MutableInteractionSource() }, indication = null) {
                focus.requestFocus()
                keyboard?.show()
            }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(label, Modifier.width(120.dp), style = TextStyle(color = colors.text, fontSize = 16.sp))
        BasicTextField(
            value,
            onChange,
            Modifier.weight(1f).focusRequester(focus).padding(vertical = 11.dp),
            singleLine = true,
            textStyle = TextStyle(color = colors.text, fontSize = 16.sp),
            cursorBrush = SolidColor(colors.tint),
        )
    }
}
