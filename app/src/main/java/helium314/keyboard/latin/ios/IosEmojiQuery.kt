// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.ios

import helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode
import helium314.keyboard.latin.common.Constants

/** While emoji search is open, typed characters edit the query instead of reaching the app. */
object IosEmojiQuery {
    sealed interface Result {
        data class Edit(val query: String) : Result
        /** close the search, the key does nothing else */
        data object Exit : Result
        /** close the search, then handle the key as usual (e.g. emoji key switches to the emoji screen) */
        data object ExitAndPassThrough : Result
        /** not about the query (shift, 123, language...): handle as usual */
        data object PassThrough : Result
    }

    fun onCode(query: String, codePoint: Int, keyCode: Int): Result = when {
        codePoint == Constants.CODE_ENTER || keyCode == KeyCode.SHIFT_ENTER -> Result.Exit
        keyCode == KeyCode.DELETE -> Result.Edit(
            if (query.isEmpty()) query else query.dropLast(Character.charCount(query.codePointBefore(query.length))))
        keyCode == KeyCode.DELETE_WORD -> Result.Edit(query.trimEnd().let { it.substring(0, it.lastIndexOf(' ') + 1) })
        keyCode == KeyCode.EMOJI || keyCode == KeyCode.CLIPBOARD -> Result.ExitAndPassThrough
        keyCode != 0 -> Result.PassThrough
        codePoint >= Constants.CODE_SPACE -> Result.Edit(query + String(Character.toChars(codePoint)))
        else -> Result.PassThrough
    }

    fun onText(query: String, text: String): Result = Result.Edit(query + text)
}
