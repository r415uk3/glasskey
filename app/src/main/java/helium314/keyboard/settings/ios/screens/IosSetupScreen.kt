// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.ios.screens

import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.inputmethod.InputMethodManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import helium314.keyboard.latin.R
import helium314.keyboard.latin.utils.UncachedInputMethodManagerUtils
import helium314.keyboard.settings.ios.IosBackground
import helium314.keyboard.settings.ios.IosGroupShape
import helium314.keyboard.settings.ios.IosSeparator
import helium314.keyboard.settings.ios.LocalIosColors
import helium314.keyboard.settings.ios.LocalIosDark
import helium314.keyboard.settings.ios.iosBackdropSource
import helium314.keyboard.settings.ios.liquid.LiquidButton
import helium314.keyboard.settings.ios.rememberIosBackdrop
import helium314.keyboard.settings.ios.IosGlass
import helium314.keyboard.settings.ios.IosNavMotion
import kotlinx.coroutines.delay

enum class IosSetupStep { WELCOME, ENABLE, DONE }

object IosSetup {
    /** Welcome only for a keyboard not even turned on yet, and only until the user moved on from it. */
    fun step(enabled: Boolean, current: Boolean, welcomeSeen: Boolean): IosSetupStep = when {
        enabled && current -> IosSetupStep.DONE
        !enabled && !welcomeSeen -> IosSetupStep.WELCOME
        else -> IosSetupStep.ENABLE
    }
}

/** What the setup needs from Android; replaced in tests. */
interface IosSetupSystem {
    fun isEnabled(): Boolean
    fun isCurrent(): Boolean
    fun openKeyboardList()
    fun showPicker()
}

private class AndroidSetupSystem(private val ctx: Context) : IosSetupSystem {
    private val imm = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    override fun isEnabled() = UncachedInputMethodManagerUtils.isThisImeEnabled(ctx, imm)
    override fun isCurrent() = UncachedInputMethodManagerUtils.isThisImeCurrent(ctx, imm)
    override fun openKeyboardList() {
        ctx.startActivity(Intent(android.provider.Settings.ACTION_INPUT_METHOD_SETTINGS).addCategory(Intent.CATEGORY_DEFAULT)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
    override fun showPicker() = imm.showInputMethodPicker()
}

const val IosSetupPrimaryTag = "ios_setup_primary"

/**
 * iOS-style keyboard setup instead of HeliBoard's wizard: welcome, turning the keyboard on (two steps that tick
 * off as Android reports them done), and a done screen with a field to try it. [onClose] leaves it.
 */
@Composable
fun IosSetupScreen(onClose: () -> Unit, system: IosSetupSystem? = null) {
    val ctx = LocalContext.current
    val sys = system ?: remember { AndroidSetupSystem(ctx) }
    var enabled by remember { mutableStateOf(sys.isEnabled()) }
    var current by remember { mutableStateOf(sys.isCurrent()) }
    var welcomeSeen by rememberSaveable { mutableStateOf(false) }
    val step = IosSetup.step(enabled, current, welcomeSeen)
    fun refresh() { enabled = sys.isEnabled(); current = sys.isCurrent() }

    // back from Android's keyboard list
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh() }
    // the keyboard picker is a dialog over this screen, so there is no resume: poll while a step is open
    LaunchedEffect(step) {
        while (step == IosSetupStep.ENABLE) {
            delay(100)
            refresh()
        }
    }
    BackHandler(onBack = onClose)

    val colors = LocalIosColors.current
    val backdrop = rememberIosBackdrop()
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(Modifier.fillMaxSize()) {
        IosBackground(LocalIosDark.current, Modifier.fillMaxSize().iosBackdropSource(backdrop))
        // pushed and popped like settings pages: the new step slides in over the old one, which moves a little
        AnimatedContent(step, transitionSpec = {
            val forward = targetState > initialState
            val spec = tween<IntOffset>(IosNavMotion.DurationMillis, easing = IosNavMotion.Easing)
            val parallax = { w: Int -> (w * IosNavMotion.ParallaxFraction).toInt() }
            (if (forward) slideInHorizontally(spec) { it } togetherWith slideOutHorizontally(spec) { -parallax(it) }
            else slideInHorizontally(spec) { -parallax(it) } togetherWith slideOutHorizontally(spec) { it })
                .apply { targetContentZIndex = if (forward) 1f else -1f }
        }, label = "setup") { s ->
            Column(Modifier.fillMaxSize().background(colors.background).padding(top = top, bottom = bottom).padding(horizontal = 24.dp)) {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    when (s) {
                        IosSetupStep.WELCOME -> Welcome()
                        IosSetupStep.ENABLE -> Enable(enabled)
                        IosSetupStep.DONE -> Done()
                    }
                }
                if (s == IosSetupStep.ENABLE)
                    BasicText(
                        stringResource(R.string.ios_later),
                        Modifier.fillMaxWidth().clickable(remember { MutableInteractionSource() }, null, role = Role.Button, onClick = onClose)
                            .padding(vertical = 14.dp),
                        style = TextStyle(color = colors.tint, fontSize = 17.sp, textAlign = TextAlign.Center),
                    )
                val (label, action) = when (s) {
                    IosSetupStep.WELCOME -> R.string.ios_continue to { welcomeSeen = true }
                    IosSetupStep.ENABLE ->
                        if (enabled) R.string.ios_setup_choose_action to { sys.showPicker() }
                        else R.string.ios_setup_enable_action to { welcomeSeen = true; sys.openKeyboardList() }
                    IosSetupStep.DONE -> R.string.ios_setup_go_to_settings to onClose
                }
                LiquidButton(
                    action,
                    backdrop,
                    Modifier.fillMaxWidth().padding(bottom = 16.dp).testTag(IosSetupPrimaryTag),
                    tint = colors.tint,
                    level = IosGlass.levelFor(Build.VERSION.SDK_INT),
                    height = 52.dp,
                ) {
                    BasicText(stringResource(label), style = TextStyle(color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold))
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.Title(text: String, center: Boolean) {
    BasicText(
        text,
        Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 6.dp),
        style = TextStyle(color = LocalIosColors.current.text, fontSize = 32.sp, fontWeight = FontWeight.Bold,
            textAlign = if (center) TextAlign.Center else TextAlign.Start),
    )
}

@Composable
private fun Subtitle(text: String, center: Boolean) {
    BasicText(
        text,
        Modifier.fillMaxWidth().padding(bottom = 24.dp),
        style = TextStyle(color = LocalIosColors.current.secondaryText, fontSize = 16.sp, lineHeight = 22.sp,
            textAlign = if (center) TextAlign.Center else TextAlign.Start),
    )
}

@Composable
private fun ColumnScope.Welcome() {
    Spacer(Modifier.height(48.dp))
    val ctx = LocalContext.current
    val px = with(LocalDensity.current) { 96.dp.roundToPx() }
    // an adaptive icon, which painterResource can't load
    val icon = remember(px) {
        runCatching { ContextCompat.getDrawable(ctx, R.mipmap.ic_launcher)?.toBitmap(px, px)?.asImageBitmap() }.getOrNull()
    }
    if (icon != null)
        Image(icon, null, Modifier.align(Alignment.CenterHorizontally).size(96.dp).clip(RoundedCornerShape(22.dp)))
    Title(stringResource(R.string.english_ime_name), center = true)
    Subtitle(stringResource(R.string.ios_setup_subtitle), center = true)
    Feature(painterResource(R.drawable.ic_setup_key), R.string.ios_setup_f1_title, R.string.ios_setup_f1)
    Feature(painterResource(R.drawable.sym_keyboard_language_switch), R.string.ios_setup_f2_title, R.string.ios_setup_f2)
    Feature(painterResource(R.drawable.ic_setup_check), R.string.ios_setup_f3_title, R.string.ios_setup_f3)
}

@Composable
private fun Feature(icon: Painter, title: Int, text: Int) {
    val colors = LocalIosColors.current
    Row(Modifier.fillMaxWidth().padding(bottom = 20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Image(icon, null, Modifier.size(32.dp), colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(colors.tint))
        Column {
            BasicText(stringResource(title), style = TextStyle(color = colors.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold))
            BasicText(stringResource(text), style = TextStyle(color = colors.secondaryText, fontSize = 15.sp, lineHeight = 20.sp))
        }
    }
}

@Composable
private fun ColumnScope.Enable(enabled: Boolean) {
    val colors = LocalIosColors.current
    Spacer(Modifier.height(40.dp))
    Title(stringResource(R.string.ios_setup_enable_title), center = false)
    Subtitle(stringResource(R.string.ios_setup_enable_subtitle), center = false)
    Column(Modifier.fillMaxWidth().background(colors.groupFill, IosGroupShape)) {
        StepRow(1, done = enabled, active = !enabled, R.string.ios_setup_step1, R.string.ios_setup_step1_hint)
        IosSeparator()
        StepRow(2, done = false, active = enabled, R.string.ios_setup_step2, R.string.ios_setup_step2_hint)
    }
    BasicText(
        stringResource(R.string.ios_setup_warning),
        Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        style = TextStyle(color = colors.secondaryText, fontSize = 13.sp, lineHeight = 18.sp),
    )
}

@Composable
private fun StepRow(number: Int, done: Boolean, active: Boolean, title: Int, hint: Int) {
    val colors = LocalIosColors.current
    Row(Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(30.dp).background(
                when { done -> colors.switchOn; active -> colors.tint; else -> colors.switchOff }, CircleShape
            ),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                if (done) "✓" else number.toString(),
                style = TextStyle(color = if (done) Color.Black else if (active) Color.White else colors.secondaryText,
                    fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.padding(vertical = 10.dp)) {
            BasicText(stringResource(title), style = TextStyle(color = colors.text, fontSize = 16.sp))
            BasicText(
                stringResource(if (done) R.string.ios_setup_step_done else hint),
                style = TextStyle(color = colors.secondaryText, fontSize = 13.sp),
            )
        }
    }
}

@Composable
private fun ColumnScope.Done() {
    val colors = LocalIosColors.current
    var text by rememberSaveable { mutableStateOf("") }
    Spacer(Modifier.height(64.dp))
    Box(Modifier.align(Alignment.CenterHorizontally).size(84.dp).background(colors.switchOn, CircleShape), contentAlignment = Alignment.Center) {
        BasicText("✓", style = TextStyle(color = Color.Black, fontSize = 44.sp, fontWeight = FontWeight.Bold))
    }
    Title(stringResource(R.string.ios_setup_done_title), center = true)
    Subtitle(stringResource(R.string.ios_setup_done_subtitle), center = true)
    Box(Modifier.fillMaxWidth().heightIn(min = 50.dp).background(colors.groupFill, RoundedCornerShape(16.dp)).padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart) {
        if (text.isEmpty())
            BasicText(stringResource(R.string.ios_setup_try_hint), style = TextStyle(color = colors.secondaryText, fontSize = 16.sp))
        BasicTextField(
            text, { text = it }, Modifier.fillMaxWidth(),
            textStyle = TextStyle(color = colors.text, fontSize = 16.sp),
            cursorBrush = SolidColor(colors.tint),
        )
    }
}
