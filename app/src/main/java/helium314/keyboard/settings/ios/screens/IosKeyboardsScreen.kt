// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.ios.screens

import android.net.Uri
import android.view.inputmethod.InputMethodSubtype
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import helium314.keyboard.latin.R
import helium314.keyboard.latin.settings.SettingsSubtype.Companion.toSettingsSubtype
import helium314.keyboard.latin.utils.SubtypeLocaleUtils
import helium314.keyboard.latin.utils.SubtypeSettings
import helium314.keyboard.latin.utils.locale
import helium314.keyboard.latin.utils.mainLayoutNameOrQwerty
import helium314.keyboard.settings.ios.IosBarButton
import helium314.keyboard.settings.ios.IosGroupBox
import helium314.keyboard.settings.ios.IosRoute
import helium314.keyboard.settings.ios.IosRow
import helium314.keyboard.settings.ios.IosScreen
import helium314.keyboard.settings.ios.IosSeparator
import helium314.keyboard.settings.ios.LocalIosColors
import helium314.keyboard.settings.ios.iosGroup
import sh.calvin.reorderable.ReorderableColumn

const val IosDeleteTag = "ios_delete"
const val IosDragHandleTag = "ios_drag_handle"

private val iosRed = Color(0xFFFF3B30)

/** The user's keyboards in globe order; «Изменить» shows delete controls and drag handles. */
@Composable
fun IosKeyboardsScreen(onBack: () -> Unit, navigate: (String) -> Unit) {
    val ctx = LocalContext.current
    var subtypes by remember { mutableStateOf(SubtypeSettings.getEnabledSubtypes().toList()) }
    var editing by rememberSaveable { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<InputMethodSubtype?>(null) }
    val canDelete = subtypes.size > 1 // at least one keyboard must stay
    val footer = stringResource(R.string.ios_keyboards_footer)
    val addKeyboard = stringResource(R.string.ios_add_keyboard)
    val dictionaries = stringResource(R.string.ios_dictionaries)
    val dictionariesFooter = stringResource(R.string.ios_dictionaries_footer)
    val tint = LocalIosColors.current.tint

    IosScreen(
        stringResource(R.string.ios_keyboards),
        onBack,
        action = {
            IosBarButton(stringResource(if (editing) R.string.ios_done else R.string.ios_edit)) {
                editing = !editing
                pendingDelete = null
            }
        },
    ) {
        item {
            IosGroupBox(header = null, footer = footer) {
                ReorderableColumn(
                    subtypes,
                    onSettle = { from, to ->
                        SubtypeSettings.moveEnabledSubtype(ctx, from, to)
                        subtypes = SubtypeSettings.getEnabledSubtypes().toList()
                    },
                ) { index, subtype, dragging ->
                    key(subtype) {
                        ReorderableItem {
                            val colors = LocalIosColors.current
                            Column(if (dragging) Modifier.background(colors.background.copy(alpha = 0.85f)) else Modifier) {
                                if (index > 0) IosSeparator()
                                KeyboardRow(
                                    subtype,
                                    editing = editing,
                                    deletable = canDelete,
                                    confirmingDelete = pendingDelete == subtype,
                                    onToggleDelete = { pendingDelete = if (pendingDelete == subtype) null else subtype },
                                    onDelete = {
                                        SubtypeSettings.removeEnabledSubtype(ctx, subtype)
                                        subtypes = SubtypeSettings.getEnabledSubtypes().toList()
                                        pendingDelete = null
                                    },
                                    onClick = { navigate(IosRoute.Layout + Uri.encode(subtype.toSettingsSubtype().toPref())) },
                                    handle = Modifier.draggableHandle(),
                                )
                            }
                        }
                    }
                }
            }
        }
        iosGroup(rows = listOf {
            IosRow(addKeyboard, titleColor = tint, chevron = false, onClick = { navigate(IosRoute.AddKeyboard) })
        })
        iosGroup(footer = dictionariesFooter, rows = listOf {
            IosRow(dictionaries, onClick = { navigate(IosRoute.Dictionaries) })
        })
    }
}

@Composable
private fun KeyboardRow(
    subtype: InputMethodSubtype,
    editing: Boolean,
    deletable: Boolean,
    confirmingDelete: Boolean,
    onToggleDelete: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    handle: Modifier,
) {
    val ctx = LocalContext.current
    val colors = LocalIosColors.current
    val locale = subtype.locale()
    val name = remember(subtype) { SubtypeLocaleUtils.getSubtypeLocaleDisplayName(locale) }
    val layout = remember(subtype) { iosLayoutLabel(ctx, subtype.mainLayoutNameOrQwerty(), locale) }
    val deleteText = stringResource(R.string.ios_delete)
    val reorderText = stringResource(R.string.ios_reorder)
    IosRow(
        name,
        value = if (confirmingDelete) null else layout,
        chevron = !editing,
        onClick = if (editing) null else onClick,
        leading = if (editing && deletable) { ->
            Box(
                Modifier
                    .testTag(IosDeleteTag)
                    .size(22.dp)
                    .background(iosRed, CircleShape)
                    .clickable(remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onToggleDelete)
                    .semantics { contentDescription = "$deleteText $name" },
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(width = 10.dp, height = 2.dp).background(Color.White, RoundedCornerShape(1.dp)))
            }
        } else null,
        trailing = if (!editing) null else { ->
            if (confirmingDelete)
                Box(
                    Modifier
                        .background(iosRed, RoundedCornerShape(percent = 50))
                        .clickable(remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onDelete)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    BasicText(deleteText, style = TextStyle(color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold))
                }
            Canvas(
                handle
                    .testTag(IosDragHandleTag)
                    .semantics { contentDescription = reorderText }
                    .padding(start = 8.dp)
                    .size(width = 20.dp, height = 14.dp)
            ) {
                for (i in 0..2) {
                    val y = size.height * (0.15f + 0.35f * i)
                    drawLine(colors.secondaryText, Offset(0f, y), Offset(size.width, y), 1.5.dp.toPx(), StrokeCap.Round)
                }
            }
        },
    )
}
