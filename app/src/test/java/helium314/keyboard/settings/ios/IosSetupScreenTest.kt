// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.ios

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import helium314.keyboard.settings.ios.screens.IosSetup
import helium314.keyboard.settings.ios.screens.IosSetupPrimaryTag
import helium314.keyboard.settings.ios.screens.IosSetupScreen
import helium314.keyboard.settings.ios.screens.IosSetupStep
import helium314.keyboard.settings.ios.screens.IosSetupSystem
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "ru-w402dp-h900dp")
class IosSetupScreenTest {
    @get:Rule val compose = createComposeRule()

    private class FakeSystem(var enabled: Boolean, var current: Boolean) : IosSetupSystem {
        val calls = mutableListOf<String>()
        var checks = 0
        override fun isEnabled() = enabled.also { checks++ }
        override fun isCurrent() = current
        override fun openKeyboardList() { calls += "list" }
        override fun showPicker() { calls += "picker" }
    }

    @Test fun stepFollowsWhatAndroidReports() {
        assertEquals(IosSetupStep.WELCOME, IosSetup.step(enabled = false, current = false, welcomeSeen = false))
        assertEquals(IosSetupStep.ENABLE, IosSetup.step(enabled = false, current = false, welcomeSeen = true))
        // turned on already: straight to choosing it, no welcome
        assertEquals(IosSetupStep.ENABLE, IosSetup.step(enabled = true, current = false, welcomeSeen = false))
        assertEquals(IosSetupStep.DONE, IosSetup.step(enabled = true, current = true, welcomeSeen = false))
    }

    @Test fun walksFromWelcomeToDone() {
        val sys = FakeSystem(enabled = false, current = false)
        var closed = 0
        compose.setContent { IosSettingsTheme(dark = true) { IosSetupScreen(onClose = { closed++ }, system = sys) } }

        compose.onNodeWithText("Клавиатура как на iPhone").assertExists()
        compose.onNodeWithTag(IosSetupPrimaryTag).performClick() // Продолжить
        compose.onNodeWithText("Включите Glasskey").assertExists()
        compose.onNodeWithText("Открыть настройки Android").performClick()
        assertEquals(listOf("list"), sys.calls)

        sys.enabled = true // the user turned it on and came back; the polling picks it up
        compose.mainClock.advanceTimeBy(300)
        compose.onNodeWithText("Выбрать клавиатуру").performClick()
        assertEquals(listOf("list", "picker"), sys.calls)

        sys.current = true // chosen in the picker dialog
        compose.mainClock.advanceTimeBy(300)
        compose.waitForIdle()
        compose.onNodeWithText("Всё готово").assertExists()
        compose.onNodeWithText("К настройкам").performClick()
        assertEquals(1, closed)
    }

    @Test fun nextStepSlidesInFromTheRight() {
        val sys = FakeSystem(enabled = false, current = false)
        compose.setContent { IosSettingsTheme(dark = true) { IosSetupScreen(onClose = {}, system = sys) } }
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag(IosSetupPrimaryTag).performClick()
        compose.mainClock.advanceTimeBy(80)
        val incoming = compose.onNodeWithText("Включите Glasskey").fetchSemanticsNode().boundsInRoot.left
        compose.onNodeWithText("Клавиатура как на iPhone").assertExists() // the old step is still there, sliding away
        compose.mainClock.advanceTimeBy(1000)
        val settled = compose.onNodeWithText("Включите Glasskey").fetchSemanticsNode().boundsInRoot.left
        assertTrue(incoming > settled + 100, "incoming $incoming, settled $settled")
    }

    // drawn over the settings pages: nothing behind it may be touched or read out
    @Test fun settingsBehindTheSetupCannotBeTouched() {
        val sys = FakeSystem(enabled = false, current = false)
        var behind = 0
        compose.setContent {
            IosSettingsTheme(dark = true) {
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize().clickable { behind++ })
                    IosSetupScreen(onClose = {}, system = sys)
                }
            }
        }
        compose.onRoot().performTouchInput { click(Offset(4f, centerY)); click(Offset(4f, 4f)) }
        assertEquals(0, behind)
    }

    @Test fun laterCloses() {
        val sys = FakeSystem(enabled = true, current = false)
        var closed = 0
        compose.setContent { IosSettingsTheme(dark = true) { IosSetupScreen(onClose = { closed++ }, system = sys) } }
        compose.onNodeWithText("Позже").performClick()
        assertEquals(1, closed)
    }

    // in the background (Android's keyboard list in front) there is nothing to see, so nothing is checked
    @Test fun noCheckingWhileTheAppIsInTheBackground() {
        val sys = FakeSystem(enabled = true, current = false)
        val owner = object : LifecycleOwner { val registry = LifecycleRegistry(this); override val lifecycle get() = registry }
        compose.runOnUiThread { owner.registry.currentState = Lifecycle.State.RESUMED }
        compose.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                IosSettingsTheme(dark = true) { IosSetupScreen(onClose = {}, system = sys) }
            }
        }
        compose.mainClock.advanceTimeBy(300)
        assertTrue(sys.checks > 1) // checks while shown (the picker dialog does not pause the activity)
        compose.runOnUiThread { owner.registry.currentState = Lifecycle.State.CREATED }
        compose.waitForIdle()
        val stopped = sys.checks
        compose.mainClock.advanceTimeBy(1000)
        assertEquals(stopped, sys.checks)
    }
}
