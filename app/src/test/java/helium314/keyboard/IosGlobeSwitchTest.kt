// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard

import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import helium314.keyboard.event.HapticEvent
import helium314.keyboard.keyboard.KeyboardElement
import helium314.keyboard.keyboard.KeyboardSwitcher
import helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode
import helium314.keyboard.latin.LatinIME
import helium314.keyboard.latin.RichInputMethodManager
import helium314.keyboard.latin.common.Constants.Separators
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.SubtypeSettings
import helium314.keyboard.latin.utils.locale
import helium314.keyboard.latin.utils.prefs
import org.junit.After
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowInputMethodService::class, ShadowLooper::class])
class IosGlobeSwitchTest {
    private val latinIME = run {
        // shipped defaults: ru, en-US, kk with ru selected (prefs survive between tests here)
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.prefs().edit { remove(Settings.PREF_ENABLED_SUBTYPES); remove(Settings.PREF_SELECTED_SUBTYPE) }
        SubtypeSettings.reloadEnabledSubtypes(context)
        Robolectric.setupService(LatinIME::class.java)
    }
    private val keyboardSwitcher = KeyboardSwitcher.getInstance()

    init {
        keyboardSwitcher.onCreateInputView(latinIME, true)
        keyboardSwitcher.reloadMainKeyboard()
    }

    private fun press(code: Int) {
        val listener = latinIME.mKeyboardActionListener
        listener.onPressKey(code, 0, 1, HapticEvent.KEY_PRESS)
        listener.onCodeInput(code, -1, -1, false)
        listener.onReleaseKey(code, false)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
    }

    private fun currentLanguage() = RichInputMethodManager.getInstance().currentSubtype.locale.toLanguageTag()

    @Test fun globeCyclesThroughAllLanguagesEvenAfterTyping() {
        val seen = mutableListOf(currentLanguage())
        repeat(3) {
            press('a'.code) // typing marks the language as used; stock HeliBoard would then toggle back
            latinIME.switchToNextSubtypeInOrder()
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
            seen.add(currentLanguage())
        }
        assertEquals(listOf("ru", "en-US", "kk", "ru"), seen)
    }

    @Test fun globeFromShiftedSymbolsGoesToNextLanguageAlphabet() {
        assertEquals("ru", currentLanguage())
        press(KeyCode.SYMBOL_ALPHA)
        press(KeyCode.SHIFT)
        assertEquals(KeyboardElement.SYMBOLS_SHIFTED, keyboardSwitcher.keyboard?.mId?.element)
        press(KeyCode.LANGUAGE_SWITCH)
        assertEquals("en-US", currentLanguage())
        assertEquals(KeyboardElement.ALPHABET, keyboardSwitcher.keyboard?.mId?.element)
    }

    // kk -> ru (the brief's example) holds for the default and the alphabetical order too, so use en-US, ru, kk
    @Test fun globeFollowsUserOrder() {
        SubtypeSettings.moveEnabledSubtype(latinIME, 1, 0) // en-US, ru, kk
        assertEquals("ru", currentLanguage())
        assertEquals("kk", RichInputMethodManager.getInstance().getNextSubtypeInThisIme(true)?.locale()?.toLanguageTag())
        latinIME.switchToNextSubtypeInOrder()
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        assertEquals("kk", currentLanguage())
        latinIME.switchToNextSubtypeInOrder()
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        assertEquals("en-US", currentLanguage())
    }

    @Test fun disablingTheCurrentLanguageOnUntouchedDefaultsSwitchesAway() {
        assertEquals("ru", currentLanguage())
        val ru = SubtypeSettings.getEnabledSubtypes()[0]
        assertTrue(SubtypeSettings.removeEnabledSubtype(latinIME, ru))
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        assertEquals(listOf("en-US", "kk"), SubtypeSettings.getEnabledSubtypes().map { it.locale().toLanguageTag() })
        assertEquals("en-US", currentLanguage())
        // switched for real, not just the fallback to the first enabled language
        assertEquals("en-US", latinIME.prefs().getString(Settings.PREF_SELECTED_SUBTYPE, "")!!.substringBefore(Separators.SET))
    }

    @After fun restoreDefaults() {
        latinIME.prefs().edit { remove(Settings.PREF_ENABLED_SUBTYPES); remove(Settings.PREF_SELECTED_SUBTYPE) }
        SubtypeSettings.reloadEnabledSubtypes(latinIME)
    }
}
