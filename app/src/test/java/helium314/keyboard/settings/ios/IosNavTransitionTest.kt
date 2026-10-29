// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.ios

import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import helium314.keyboard.settings.SettingsActivity
import helium314.keyboard.settings.SettingsContainer
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "ru-w402dp-h2000dp")
class IosNavTransitionTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Before fun setUp() {
        SettingsActivity.settingsContainer = SettingsContainer(compose.activity)
    }

    private lateinit var stack: IosNavStack

    private fun showApp(scale: Float = 1f) = compose.setContent {
        stack = rememberIosNavStack()
        IosSettingsTheme(dark = true) {
            IosSettingsNavHost(onClickBack = {}, needsEnabling = false, onEnable = {}, animationScale = scale, stack = stack)
        }
    }

    private fun titleNodes(): List<SemanticsNode> = compose.onAllNodesWithTag(IosLargeTitleTag).fetchSemanticsNodes()
    private fun SemanticsNode.text() = config[SemanticsProperties.Text].joinToString()
    private fun titles() = titleNodes().map { it.text() }
    // the title's bounds include its padding, so its left edge is the page's
    private fun pageLeft(title: String) = titleNodes().first { it.text() == title }.boundsInRoot.left
    private val width get() = compose.onRoot().fetchSemanticsNode().size.width.toFloat()

    private fun openKeyboards() {
        compose.onNodeWithText("Клавиатуры").performClick()
        compose.waitForIdle()
        assertEquals(listOf("Клавиатуры"), titles())
    }

    @Test fun pushSlidesNewScreenInOverTheOldOne() {
        showApp()
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        compose.onNodeWithText("Клавиатуры").performClick()
        compose.mainClock.advanceTimeBy(150)
        assertEquals(setOf("Glasskey", "Клавиатуры"), titles().toSet())
        // the incoming page is on its way in from the trailing edge, the old one is moving toward the leading edge
        val incoming = pageLeft("Клавиатуры")
        val outgoing = pageLeft("Glasskey")
        assertTrue(incoming > 0.02f * width && incoming < 0.8f * width, "incoming at $incoming of $width")
        assertTrue(outgoing < 0f && outgoing > -0.31f * width, "outgoing at $outgoing of $width")
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        assertEquals(listOf("Клавиатуры"), titles())
        assertTrue(abs(pageLeft("Клавиатуры")) < 1f, "settled at ${pageLeft("Клавиатуры")}")
    }

    @Test fun backButtonPopsWithTheReverseAnimation() {
        showApp()
        openKeyboards()
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag(IosBackButtonTag).performClick()
        compose.mainClock.advanceTimeBy(150)
        assertEquals(setOf("Glasskey", "Клавиатуры"), titles().toSet())
        val leaving = pageLeft("Клавиатуры")
        val returning = pageLeft("Glasskey")
        assertTrue(leaving > 0.02f * width, "leaving at $leaving")
        assertTrue(returning < 0f && returning > -0.31f * width, "returning at $returning")
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        assertEquals(listOf("Glasskey"), titles())
    }

    @Test fun zeroScaleNavigatesImmediately() {
        showApp(scale = 0f)
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        compose.onNodeWithText("Клавиатуры").performClick()
        compose.mainClock.advanceTimeBy(50)
        assertEquals(listOf("Клавиатуры"), titles())
    }

    @Test fun oneSharedBackgroundDuringTransition() {
        showApp()
        compose.waitForIdle()
        assertEquals(1, compose.onAllNodesWithTag(IosBackgroundTag, useUnmergedTree = true).fetchSemanticsNodes().size)
        compose.mainClock.autoAdvance = false
        compose.onNodeWithText("Клавиатуры").performClick()
        compose.mainClock.advanceTimeBy(150)
        assertEquals(2, titles().size)
        assertEquals(1, compose.onAllNodesWithTag(IosBackgroundTag, useUnmergedTree = true).fetchSemanticsNodes().size)
    }

    @Test fun predictiveBackFollowsTheFingerAndCanBeCancelled() {
        showApp()
        openKeyboards()
        val dispatcher = compose.activity.onBackPressedDispatcher
        compose.runOnUiThread {
            dispatcher.dispatchOnBackStarted(BackEventCompat(0f, 500f, 0f, BackEventCompat.EDGE_LEFT))
            dispatcher.dispatchOnBackProgressed(BackEventCompat(200f, 500f, 0.5f, BackEventCompat.EDGE_LEFT))
        }
        compose.waitForIdle()
        assertEquals(setOf("Glasskey", "Клавиатуры"), titles().toSet())
        // linear while dragging: half the gesture moves the page half the width
        val dragged = pageLeft("Клавиатуры")
        assertTrue(abs(dragged - 0.5f * width) < 0.05f * width, "dragged to $dragged of $width")
        compose.runOnUiThread { dispatcher.dispatchOnBackCancelled() }
        compose.waitForIdle()
        assertEquals(listOf("Клавиатуры"), titles())

        compose.runOnUiThread {
            dispatcher.dispatchOnBackStarted(BackEventCompat(0f, 500f, 0f, BackEventCompat.EDGE_LEFT))
            dispatcher.dispatchOnBackProgressed(BackEventCompat(100f, 500f, 0.3f, BackEventCompat.EDGE_LEFT))
            dispatcher.onBackPressed()
        }
        compose.waitForIdle()
        assertEquals(listOf("Glasskey"), titles())
    }

    private fun back(x: Float, progress: Float) = BackEventCompat(x, 500f, progress, BackEventCompat.EDGE_LEFT)

    // the page below stays composed, so going back moves both pages on the very next frame
    @Test fun popMovesOnTheNextFrame() {
        showApp()
        openKeyboards()
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag(IosBackButtonTag).performClick()
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeByFrame()
        assertEquals(setOf("Glasskey", "Клавиатуры"), titles().toSet())
        assertTrue(pageLeft("Клавиатуры") > 0f, "leaving at ${pageLeft("Клавиатуры")}")
        assertTrue(pageLeft("Glasskey") > -0.3f * width, "returning at ${pageLeft("Glasskey")}")
    }

    // the user's video: the back swipe jumped half way at once. The page's edge stays under the finger, whatever
    // the system's progress says
    @Test fun backGestureKeepsThePageUnderTheFinger() {
        showApp()
        openKeyboards()
        val dispatcher = compose.activity.onBackPressedDispatcher
        compose.runOnUiThread {
            dispatcher.dispatchOnBackStarted(back(0f, 0f))
            dispatcher.dispatchOnBackProgressed(back(40f, 0.6f))
        }
        compose.waitForIdle()
        assertTrue(abs(pageLeft("Клавиатуры") - 40f) < 2f, "at ${pageLeft("Клавиатуры")}, finger at 40")
        assertTrue(abs(pageLeft("Glasskey") - (-0.3f * width * (1 - 40f / width))) < 2f, "below at ${pageLeft("Glasskey")}")
        compose.runOnUiThread { dispatcher.dispatchOnBackProgressed(back(120f, 0.7f)) }
        compose.waitForIdle()
        assertTrue(abs(pageLeft("Клавиатуры") - 120f) < 2f, "at ${pageLeft("Клавиатуры")}, finger at 120")
        compose.runOnUiThread { dispatcher.dispatchOnBackCancelled() }
        compose.waitForIdle()
        assertEquals(listOf("Клавиатуры"), titles())
        assertTrue(abs(pageLeft("Клавиатуры")) < 1f)
    }

    // a back swipe right after a push catches the page where it is instead of jumping
    @Test fun backGestureDuringAPushDoesNotJump() {
        showApp()
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        compose.onNodeWithText("Клавиатуры").performClick()
        compose.mainClock.advanceTimeBy(100)
        val before = pageLeft("Клавиатуры")
        assertTrue(before > 0.05f * width, "still sliding in: $before")
        val dispatcher = compose.activity.onBackPressedDispatcher
        compose.runOnUiThread {
            dispatcher.dispatchOnBackStarted(back(0f, 0f))
            dispatcher.dispatchOnBackProgressed(back(10f, 0.1f)) // finger behind the page
        }
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeByFrame()
        assertTrue(abs(pageLeft("Клавиатуры") - before) < 2f, "jumped from $before to ${pageLeft("Клавиатуры")}")
        compose.runOnUiThread { dispatcher.onBackPressed() }
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        assertEquals(listOf("Glasskey"), titles())
        assertEquals(1, stack.entries.size)
    }

    // the user taps a row again while the page is still moving: one push, no second page
    @Test fun tapsDuringATransitionAreIgnored() {
        showApp()
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        compose.onNodeWithText("Клавиатуры").performClick()
        compose.mainClock.advanceTimeBy(50)
        compose.onNodeWithText("Замена текста").performClick()
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        assertEquals(listOf(IosRoute.Main, IosRoute.Keyboards), stack.entries.map { it.route })
        assertEquals(listOf("Клавиатуры"), titles())
    }

    // pages below the top one stay composed: back shows them as they were (here: the search text)
    @Test fun pageBelowKeepsItsState() {
        showApp()
        compose.waitForIdle()
        compose.onNode(hasSetTextAction()).performTextInput("звук")
        compose.waitForIdle()
        compose.onNodeWithText("Звук").assertExists()
        // the search shows only matching switches; Keyboards is reached via the stack directly
        compose.runOnUiThread { stack.push(IosRoute.Keyboards) }
        compose.waitForIdle()
        assertEquals(listOf("Клавиатуры"), titles())
        compose.onNodeWithTag(IosBackButtonTag).performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Звук").assertExists()
        compose.onNodeWithText("Клавиатуры").assertDoesNotExist()
    }

    // the pages behind the main page's rows are prepared while the user does nothing, and a push uses them, so its
    // first frame only has to draw; prepared pages are not visible
    @Test fun pushUsesAPagePreparedAheadOfTime() {
        showApp()
        compose.mainClock.advanceTimeBy(IosNavMotion.PrewarmIdleMillis * 6)
        compose.waitForIdle()
        assertEquals(IosNavMotion.prewarmRoutes(IosRoute.Main).toSet(), stack.spares.map { it.route }.toSet())
        assertEquals(listOf("Glasskey"), titles())
        val spare = stack.spares.first { it.route == IosRoute.Keyboards }
        openKeyboards()
        assertTrue(stack.entries.last() === spare)
        assertTrue(stack.spares.none { it.route == IosRoute.Keyboards })
    }
}
