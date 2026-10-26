// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard

import android.content.Context
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodSubtype
import helium314.keyboard.keyboard.Keyboard
import helium314.keyboard.keyboard.KeyboardElement
import helium314.keyboard.keyboard.KeyboardId
import helium314.keyboard.keyboard.KeyboardLayoutSet
import helium314.keyboard.keyboard.KeyboardMode
import helium314.keyboard.keyboard.internal.KeyboardBuilder
import helium314.keyboard.keyboard.internal.KeyboardParams
import helium314.keyboard.keyboard.internal.UniqueKeysCache
import helium314.keyboard.keyboard.Key.KeyParams
import helium314.keyboard.latin.LatinIME
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import helium314.keyboard.latin.RichInputMethodSubtype
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.SubtypeSettings
import helium314.keyboard.latin.utils.prefs

/** Builds a full keyboard like [KeyboardLayoutSet] does, returning it with the rows of key params (incl. spacers). */
fun buildTestKeyboard(
    latinIME: LatinIME,
    editorInfo: EditorInfo,
    subtype: InputMethodSubtype,
    element: KeyboardElement,
    width: Int = 500,
    height: Int = 300,
    mode: KeyboardMode = KeyboardMode.TEXT,
): Pair<Keyboard, List<List<KeyParams>>> {
    val layoutParams = KeyboardLayoutSet.Params()
    fun set(name: String, value: Any) = KeyboardLayoutSet.Params::class.java.getDeclaredField(name)
        .apply { isAccessible = true }.set(layoutParams, value)
    set("editorInfo", editorInfo)
    set("subtype", RichInputMethodSubtype.get(subtype))
    set("keyboardWidth", width)
    set("keyboardHeight", height)
    layoutParams.mode = mode

    val keysInRowsField = KeyboardBuilder::class.java.getDeclaredField("keysInRows").apply { isAccessible = true }
    val id = KeyboardId(element, layoutParams)
    val builder = KeyboardBuilder(latinIME, KeyboardParams(UniqueKeysCache.NO_CACHE))
    builder.load(id)
    @Suppress("UNCHECKED_CAST")
    return builder.build() to keysInRowsField.get(builder) as ArrayList<ArrayList<KeyParams>>
}

/** Upstream HeliBoard tests assume the stock defaults (system locale = en-US, no preset languages).
 *  Glasskey presets ru/en/kk, so those tests reset the language prefs before the service starts. */
fun useStockSubtypeDefaults() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    context.prefs().edit {
        putString(Settings.PREF_ENABLED_SUBTYPES, "")
        putString(Settings.PREF_SELECTED_SUBTYPE, "")
    }
    SubtypeSettings.reloadEnabledSubtypes(context) // App.onCreate already loaded the Glasskey presets
}
