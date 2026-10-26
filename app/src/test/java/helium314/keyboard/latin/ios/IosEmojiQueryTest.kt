// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.ios

import helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode
import helium314.keyboard.latin.ios.IosEmojiQuery.Result
import kotlin.test.Test
import kotlin.test.assertEquals

class IosEmojiQueryTest {
    private fun code(query: String, codePoint: Int) = IosEmojiQuery.onCode(query, codePoint, 0)
    private fun key(query: String, keyCode: Int) = IosEmojiQuery.onCode(query, -1, keyCode)

    @Test fun lettersAndSpaceAreAppended() {
        assertEquals(Result.Edit("улы"), code("ул", 'ы'.code))
        assertEquals(Result.Edit("ту "), code("ту", ' '.code))
        assertEquals(Result.Edit("ту қ"), code("ту ", 'қ'.code))
    }

    @Test fun deleteRemovesTheLastCharacter() {
        assertEquals(Result.Edit("ул"), key("улы", KeyCode.DELETE))
        assertEquals(Result.Edit(""), key("", KeyCode.DELETE))
        assertEquals(Result.Edit("a"), key("a😀", KeyCode.DELETE)) // surrogate pair removed as a whole
        assertEquals(Result.Edit("флаг "), key("флаг каз", KeyCode.DELETE_WORD))
    }

    @Test fun enterExits() {
        assertEquals(Result.Exit, code("кот", '\n'.code))
        assertEquals(Result.Exit, key("кот", KeyCode.SHIFT_ENTER))
    }

    @Test fun emojiKeyExitsAndStillSwitches() {
        assertEquals(Result.ExitAndPassThrough, key("кот", KeyCode.EMOJI))
        assertEquals(Result.ExitAndPassThrough, key("кот", KeyCode.CLIPBOARD))
    }

    @Test fun otherFunctionalKeysWorkAsUsual() {
        for (k in listOf(KeyCode.SHIFT, KeyCode.SYMBOL, KeyCode.ALPHA, KeyCode.LANGUAGE_SWITCH))
            assertEquals(Result.PassThrough, key("кот", k))
    }

    @Test fun textIsAppended() {
        assertEquals(Result.Edit("a.com"), IosEmojiQuery.onText("a", ".com"))
    }
}
