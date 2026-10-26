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
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.SubtypeSettings
import helium314.keyboard.latin.utils.prefs
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import kotlin.test.Test
import kotlin.test.assertEquals

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

    @Test fun globeFromShiftedSymbolsGoesToNextLanguageAlphabet() {
        assertEquals("ru", currentLanguage())
        press(KeyCode.SYMBOL_ALPHA)
        press(KeyCode.SHIFT)
        assertEquals(KeyboardElement.SYMBOLS_SHIFTED, keyboardSwitcher.keyboard?.mId?.element)
        press(KeyCode.LANGUAGE_SWITCH)
        assertEquals("en-US", currentLanguage())
        assertEquals(KeyboardElement.ALPHABET, keyboardSwitcher.keyboard?.mId?.element)
    }
}
