// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard

import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodSubtype
import helium314.keyboard.keyboard.Keyboard
import helium314.keyboard.keyboard.KeyboardElement
import helium314.keyboard.keyboard.KeyboardId
import helium314.keyboard.keyboard.KeyboardLayoutSet
import helium314.keyboard.keyboard.internal.KeyboardBuilder
import helium314.keyboard.keyboard.internal.KeyboardParams
import helium314.keyboard.keyboard.internal.UniqueKeysCache
import helium314.keyboard.keyboard.Key.KeyParams
import helium314.keyboard.latin.LatinIME
import helium314.keyboard.latin.RichInputMethodSubtype

/** Builds a full keyboard like [KeyboardLayoutSet] does, returning it with the rows of key params (incl. spacers). */
fun buildTestKeyboard(
    latinIME: LatinIME,
    editorInfo: EditorInfo,
    subtype: InputMethodSubtype,
    element: KeyboardElement,
    width: Int = 500,
    height: Int = 300,
): Pair<Keyboard, List<List<KeyParams>>> {
    val layoutParams = KeyboardLayoutSet.Params()
    fun set(name: String, value: Any) = KeyboardLayoutSet.Params::class.java.getDeclaredField(name)
        .apply { isAccessible = true }.set(layoutParams, value)
    set("editorInfo", editorInfo)
    set("subtype", RichInputMethodSubtype.get(subtype))
    set("keyboardWidth", width)
    set("keyboardHeight", height)

    val keysInRowsField = KeyboardBuilder::class.java.getDeclaredField("keysInRows").apply { isAccessible = true }
    val id = KeyboardId(element, layoutParams)
    val builder = KeyboardBuilder(latinIME, KeyboardParams(UniqueKeysCache.NO_CACHE))
    builder.load(id)
    @Suppress("UNCHECKED_CAST")
    return builder.build() to keysInRowsField.get(builder) as ArrayList<ArrayList<KeyParams>>
}
