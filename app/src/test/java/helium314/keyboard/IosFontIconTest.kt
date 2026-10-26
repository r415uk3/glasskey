// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard

import android.graphics.Typeface
import androidx.core.content.edit
import helium314.keyboard.keyboard.KeyboardTypeface
import helium314.keyboard.keyboard.internal.KeyboardIconsSet
import helium314.keyboard.latin.LatinIME
import helium314.keyboard.latin.R
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.KeyboardFont
import helium314.keyboard.latin.utils.prefs
import org.junit.After
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowInputMethodManager2::class])
class IosFontIconTest {
    private val latinIME = Robolectric.setupService(LatinIME::class.java)

    @After fun tearDown() {
        Settings.getCustomFontFile(latinIME).delete()
        KeyboardTypeface.clearCache()
    }

    private fun setFont(value: String) {
        latinIME.prefs().edit { putString(Settings.PREF_KEYBOARD_FONT, value) }
        KeyboardTypeface.clearCache()
    }

    @Test fun defaultFontIsInter() {
        latinIME.prefs().edit { remove(Settings.PREF_KEYBOARD_FONT) }
        KeyboardTypeface.clearCache()
        assertEquals(KeyboardFont.INTER, KeyboardFont.fromPref(latinIME.prefs().getString(Settings.PREF_KEYBOARD_FONT, Defaults.PREF_KEYBOARD_FONT)))
        assertNotSame(Typeface.DEFAULT, KeyboardTypeface.resolve("a", Typeface.DEFAULT))
    }

    @Test fun systemFontKeepsDefault() {
        setFont("system")
        assertSame(Typeface.DEFAULT, KeyboardTypeface.resolve("a", Typeface.DEFAULT))
    }

    @Test fun unknownPrefFallsBackToInter() {
        assertEquals(KeyboardFont.INTER, KeyboardFont.fromPref("comic"))
        assertEquals(KeyboardFont.INTER, KeyboardFont.fromPref(null))
    }

    @Test fun everyFontLoads() {
        for (font in KeyboardFont.entries) {
            setFont(font.prefValue)
            val typeface = KeyboardTypeface.resolve("ә", Typeface.DEFAULT)
            if (font == KeyboardFont.SYSTEM) assertSame(Typeface.DEFAULT, typeface)
            else assertNotSame(Typeface.DEFAULT, typeface, font.name)
        }
    }

    @Test fun customFontStillWins() {
        setFont("onest")
        val selected = KeyboardTypeface.resolve("a", Typeface.DEFAULT)
        latinIME.resources.openRawResource(R.font.golos_text_regular).use { input ->
            Settings.getCustomFontFile(latinIME).outputStream().use { input.copyTo(it) }
        }
        KeyboardTypeface.clearCache()
        assertNotSame(selected, KeyboardTypeface.resolve("a", Typeface.DEFAULT))
    }

    @Test fun iosIconsUseIosDrawables() {
        val icons = KeyboardIconsSet.getAllIcons(latinIME)
        val expected = mapOf(
            "shift_key" to R.drawable.ic_ios_shift,
            "shift_key_shifted" to R.drawable.ic_ios_shift_filled,
            "shift_key_locked" to R.drawable.ic_ios_caps_lock,
            "delete_key" to R.drawable.ic_ios_delete,
            "enter_key" to R.drawable.ic_ios_return,
            "go_key" to R.drawable.ic_ios_go,
            "search_key" to R.drawable.ic_ios_search,
            "language_switch_key" to R.drawable.ic_ios_globe,
            "emoji" to R.drawable.ic_ios_emoji,
            "voice" to R.drawable.ic_ios_mic,
        )
        for ((name, id) in expected) assertEquals(id, icons[name]?.first(), name)
    }
}
