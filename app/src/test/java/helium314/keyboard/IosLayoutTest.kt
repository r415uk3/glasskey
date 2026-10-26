// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard

import android.text.InputType
import android.view.inputmethod.EditorInfo
import helium314.keyboard.keyboard.Key
import helium314.keyboard.keyboard.KeyboardElement
import helium314.keyboard.keyboard.KeyboardMode
import helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode
import helium314.keyboard.latin.LatinIME
import helium314.keyboard.latin.common.Constants
import helium314.keyboard.latin.utils.SubtypeUtilsAdditional
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowInputMethodManager2::class, ShadowProximityInfo::class])
class IosLayoutTest {
    private val latinIME = Robolectric.setupService(LatinIME::class.java)

    private fun rows(
        element: KeyboardElement = KeyboardElement.ALPHABET,
        locale: Locale = Locale.ENGLISH,
        mainLayout: String = "qwerty",
        mode: KeyboardMode = KeyboardMode.TEXT,
        editorInfo: EditorInfo = EditorInfo(),
    ): List<List<Key>> {
        val subtype = SubtypeUtilsAdditional.createEmojiCapableAdditionalSubtype(locale, mainLayout, true)
        val keyboard = buildTestKeyboard(latinIME, editorInfo, subtype, element, mode = mode).first
        return keyboard.sortedKeys.filter { !it.isSpacer }.groupBy { it.y }.toSortedMap().values.map { row -> row.sortedBy { it.x } }
    }

    private fun List<Key>.codes() = map { it.code }

    private val enter = Constants.CODE_ENTER

    @Test fun enBottomRow() {
        assertEquals(listOf(KeyCode.SYMBOL_ALPHA, KeyCode.EMOJI, ' '.code, enter), rows().last().codes())
    }

    @Test fun ruBottomRowSameAsEn() {
        val rows = rows(locale = Locale.forLanguageTag("ru"), mainLayout = "russian")
        assertEquals(listOf(KeyCode.SYMBOL_ALPHA, KeyCode.EMOJI, ' '.code, enter), rows.last().codes())
        assertEquals(listOf("й", "ц", "у", "к", "е", "н"), rows.first().take(6).map { it.label })
    }

    @Test fun thirdRowHasShiftAndDeleteAround() {
        val row = rows()[2]
        assertEquals(KeyCode.SHIFT, row.first().code)
        assertEquals(KeyCode.DELETE, row.last().code)
    }

    @Test fun kkPopups() {
        val keys = rows(locale = Locale.forLanguageTag("kk"), mainLayout = "russian").flatten()
        fun popups(label: String) = keys.first { it.label == label }.popupKeys?.map { it.mLabel }.orEmpty()
        assertTrue("ә" in popups("а"), popups("а").toString())
        assertTrue("қ" in popups("к"), popups("к").toString())
        assertFalse(popups("а").any { it?.firstOrNull()?.isDigit() == true }, popups("а").toString())
    }

    @Test fun emailRow() {
        val row = rows(mode = KeyboardMode.EMAIL).last()
        assertEquals(listOf(KeyCode.SYMBOL_ALPHA, KeyCode.EMOJI, ' '.code, '@'.code, '.'.code, enter), row.codes())
    }

    @Test fun urlRow() {
        val row = rows(mode = KeyboardMode.URL).last()
        assertEquals(listOf(KeyCode.SYMBOL_ALPHA, KeyCode.EMOJI, '.'.code, '/'.code), row.codes().take(4))
        assertEquals(".com", row[4].label)
        assertEquals(enter, row.last().code)
        assertFalse(row.any { it.code == ' '.code })
    }

    @Test fun searchRow() {
        val editorInfo = EditorInfo().apply { imeOptions = EditorInfo.IME_ACTION_SEARCH }
        val row = rows(editorInfo = editorInfo).last()
        assertEquals(listOf(KeyCode.SYMBOL_ALPHA, KeyCode.EMOJI, ' '.code, '.'.code, enter), row.codes())
    }

    @Test fun symbolsPage() {
        val rows = rows(element = KeyboardElement.SYMBOLS)
        assertEquals(listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"), rows[0].map { it.label })
        assertTrue(rows[1].any { it.label == "₸" })
        assertEquals("ABC", rows.last().first().label)
    }

    @Test fun symbolsToggleLabels() {
        assertEquals("123", rows().last().first().label)
        assertEquals("#+=", rows(element = KeyboardElement.SYMBOLS)[2].first().label)
        assertEquals("123", rows(element = KeyboardElement.SYMBOLS_SHIFTED)[2].first().label)
    }

    private fun numberRows(inputType: Int, imeOptions: Int = 0) = rows(element = KeyboardElement.NUMBER, mode = KeyboardMode.NUMBER,
        editorInfo = EditorInfo().apply { this.inputType = inputType; this.imeOptions = imeOptions })

    @Test fun decimalNumberFieldHasPeriodAndMinus() {
        val codes = numberRows(InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED).flatten().codes()
        assertTrue('.'.code in codes, codes.toString())
        assertTrue('-'.code in codes, codes.toString())
    }

    @Test fun dateAndTimeFieldsHaveSeparators() {
        val date = rows(element = KeyboardElement.NUMBER, mode = KeyboardMode.DATE).flatten().codes()
        assertTrue('/'.code in date, date.toString())
        val time = rows(element = KeyboardElement.NUMBER, mode = KeyboardMode.TIME).flatten().codes()
        assertTrue(':'.code in time, time.toString())
    }

    @Test fun numberAndPhoneFieldsCanSubmit() {
        val number = numberRows(InputType.TYPE_CLASS_NUMBER, EditorInfo.IME_ACTION_NEXT).flatten()
        assertTrue(number.any { it.hasActionKeyBackground() || it.code == Constants.CODE_ENTER }, number.map { it.code }.toString())
        val phone = rows(element = KeyboardElement.PHONE, mode = KeyboardMode.PHONE,
            editorInfo = EditorInfo().apply { inputType = InputType.TYPE_CLASS_PHONE; imeOptions = EditorInfo.IME_ACTION_DONE }).flatten()
        assertTrue(phone.any { it.hasActionKeyBackground() || it.code == Constants.CODE_ENTER }, phone.map { it.code }.toString())
    }

    @Test fun phoneFieldHasDigitsAndDelete() {
        val rows = rows(element = KeyboardElement.PHONE, mode = KeyboardMode.PHONE,
            editorInfo = EditorInfo().apply { inputType = InputType.TYPE_CLASS_PHONE })
        assertEquals(4, rows.size)
        assertEquals(listOf("1", "2", "3"), rows[0].take(3).map { it.label })
        assertTrue(rows.flatten().any { it.code == KeyCode.DELETE })
    }

    @Test fun noShiftOrLanguageKeyOnNumpad() {
        for (element in listOf(KeyboardElement.PHONE, KeyboardElement.NUMBER)) {
            val codes = rows(element = element).flatten().codes()
            assertFalse(KeyCode.SHIFT in codes, element.name)
            assertFalse(KeyCode.LANGUAGE_SWITCH in codes, element.name)
        }
    }

    @Test fun lettersHaveNoNumberOrSymbolPopups() {
        val keys = rows().flatten()
        fun popups(label: String) = keys.first { it.label == label }.popupKeys?.map { it.mLabel }.orEmpty()
        assertFalse("1" in popups("q"), popups("q").toString())
        assertFalse("@" in popups("a"), popups("a").toString())
    }

    @Test fun plainEnterIsFunctionalColoredButGoIsAction() {
        val enterKey = rows().last().last()
        assertTrue(enterKey.hasFunctionalBackground(), "plain enter")
        val goKey = rows(editorInfo = EditorInfo().apply { imeOptions = EditorInfo.IME_ACTION_GO }).last().last()
        assertTrue(goKey.hasActionKeyBackground(), "go")
    }
}
