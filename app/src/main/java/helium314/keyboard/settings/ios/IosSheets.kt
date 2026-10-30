// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.ios

import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import helium314.keyboard.latin.R
import kotlinx.coroutines.android.awaitFrame

const val IosSheetTag = "ios_sheet"

/**
 * iOS-style card with Отменить / [title] / [confirm] over the page; fades and scales in, and out again before
 * [onDismiss] (also after [onConfirm]) removes it. With [focus], that field gets the focus and the keyboard shows.
 */
@Composable
fun IosSheetCard(
    title: String,
    confirm: String,
    canConfirm: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    focus: FocusRequester? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalIosColors.current
    // heading for shown from the start, so the content is composed (and focusable) in the dialog's first frame
    val shown = remember { MutableTransitionState(false).apply { targetState = true } }
    var closing by remember { mutableStateOf(false) }
    val close = { closing = true; shown.targetState = false }
    LaunchedEffect(shown.isIdle, shown.currentState) { if (closing && shown.isIdle && !shown.currentState) onDismiss() }
    val spec = tween<Float>(IosSheetMotion.DurationMillis)

    Dialog(close, DialogProperties(usePlatformDefaultWidth = false)) {
        // focus and keyboard must be requested from inside the dialog window, once its content is attached
        val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
        // the card animates itself (below) instead of the platform's dialog window animation
        SideEffect { dialogWindow?.setWindowAnimations(0) }
        val keyboard = LocalSoftwareKeyboardController.current
        if (focus != null) LaunchedEffect(Unit) {
            dialogWindow?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            awaitFrame()
            focus.requestFocus()
            keyboard?.show()
        }
        AnimatedVisibility(
            shown,
            enter = fadeIn(spec) + scaleIn(spec, initialScale = IosSheetMotion.InitialScale),
            exit = fadeOut(spec) + scaleOut(spec, targetScale = IosSheetMotion.InitialScale),
        ) {
            Column(
                Modifier
                    .testTag(IosSheetTag)
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .background(colors.sheet, IosGroupShape)
                    .padding(bottom = 16.dp)
            ) {
                Box(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp)) {
                    SheetButton(stringResource(R.string.ios_cancel), colors.tint, FontWeight.Normal, Modifier.align(Alignment.CenterStart), close)
                    BasicText(
                        title,
                        Modifier.align(Alignment.Center),
                        style = TextStyle(color = colors.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
                    )
                    SheetButton(
                        confirm,
                        if (canConfirm) colors.tint else colors.secondaryText,
                        FontWeight.SemiBold,
                        Modifier.align(Alignment.CenterEnd),
                    ) {
                        if (canConfirm && !closing) {
                            onConfirm()
                            close()
                        }
                    }
                }
                content()
            }
        }
    }
}

@Composable
private fun SheetButton(text: String, color: Color, weight: FontWeight, modifier: Modifier, onClick: () -> Unit) {
    BasicText(
        text,
        modifier
            .clickable(remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick)
            .padding(vertical = 8.dp),
        style = TextStyle(color = color, fontSize = 17.sp, fontWeight = weight),
    )
}

private val actionShape = RoundedCornerShape(20.dp)

/**
 * iOS action sheet at the bottom: [message] over a red [action] (none for a plain notice), and [cancel] apart below.
 * Slides up, and down again before [onDismiss] (also after [onAction]) removes it.
 */
@Composable
fun IosActionSheet(
    message: String,
    action: String?,
    onAction: () -> Unit,
    onDismiss: () -> Unit,
    cancel: String = stringResource(R.string.ios_cancel),
) {
    val colors = LocalIosColors.current
    val shown = remember { MutableTransitionState(false).apply { targetState = true } }
    var closing by remember { mutableStateOf(false) }
    val close = { closing = true; shown.targetState = false }
    LaunchedEffect(shown.isIdle, shown.currentState) { if (closing && shown.isIdle && !shown.currentState) onDismiss() }
    val spec = tween<androidx.compose.ui.unit.IntOffset>(IosSheetMotion.DurationMillis)

    Dialog(close, DialogProperties(usePlatformDefaultWidth = false)) {
        val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { dialogWindow?.setWindowAnimations(0) }
        Box(
            Modifier
                .fillMaxSize()
                .clickable(remember { MutableInteractionSource() }, indication = null, onClick = close),
            contentAlignment = Alignment.BottomCenter,
        ) {
            AnimatedVisibility(
                shown,
                enter = slideInVertically(spec) { it },
                exit = slideOutVertically(spec) { it },
            ) {
                Column(
                    Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 10.dp, vertical = 10.dp)
                        .fillMaxWidth()
                ) {
                    Column(Modifier.fillMaxWidth().background(colors.sheet, actionShape)) {
                        BasicText(
                            message,
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                            style = TextStyle(color = colors.secondaryText, fontSize = 13.sp, lineHeight = 17.sp, textAlign = TextAlign.Center),
                        )
                        if (action != null) {
                            IosSeparator()
                            ActionButton(action, IosDestructive, FontWeight.Normal) {
                                if (!closing) { onAction(); close() }
                            }
                        }
                    }
                    Box(Modifier.padding(top = 8.dp).fillMaxWidth().background(colors.sheet, actionShape)) {
                        ActionButton(cancel, colors.tint, FontWeight.SemiBold, close)
                    }
                }
            }
        }
    }
}

/** iOS system red, for delete actions. */
val IosDestructive = Color(0xFFFF453A)

@Composable
private fun ActionButton(text: String, color: Color, weight: FontWeight, onClick: () -> Unit) {
    BasicText(
        text,
        Modifier
            .fillMaxWidth()
            .clickable(remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick)
            .padding(vertical = 17.dp),
        style = TextStyle(color = color, fontSize = 18.sp, fontWeight = weight, textAlign = TextAlign.Center),
    )
}
