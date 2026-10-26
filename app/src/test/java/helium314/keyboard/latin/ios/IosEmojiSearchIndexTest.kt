// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.ios

import kotlin.test.Test
import kotlin.test.assertEquals

class IosEmojiSearchIndexTest {
    private val index = IosEmojiSearchIndex.of(
        listOf(
            "😀\tgrinning face\tface|grin|smile",
            "😊\tsmiling face with smiling eyes\tblush|eye|face",
            "🇰🇿\tflag: Kazakhstan\tflag",
        ),
        listOf(
            "😀\tшироко улыбается\tлицо|радость|улыбка",
            "😊\tулыбается глазами\tлицо|улыбка",
            "🇰🇿\tфлаг: Казахстан\tфлаг",
            "🪨\tкамень\tгорная порода|твёрдый",
        ),
        listOf("🇰🇿\tту: Қазақстан\tту"),
    )

    @Test fun prefixOfANameWordMatches() {
        assertEquals(listOf("😀", "😊"), index.search("улыб"))
        assertEquals(listOf("😀"), index.search("grin"))
    }

    @Test fun everyQueryWordMustMatch() {
        assertEquals(listOf("🇰🇿"), index.search("флаг каз"))
        assertEquals(emptyList(), index.search("флаг улыб"))
    }

    @Test fun caseAndYoAreIgnored() {
        assertEquals(listOf("🪨"), index.search("ТВЕРД"))
        assertEquals(listOf("🪨"), index.search("твёрд"))
    }

    @Test fun nameMatchesComeBeforeKeywordOnlyMatches() {
        // "smil" is in the name of 😊 but only a keyword of 😀
        assertEquals(listOf("😊", "😀"), index.search("smil"))
    }

    @Test fun kazakhNamesAreSearched() {
        assertEquals(listOf("🇰🇿"), index.search("ту қаз"))
    }

    @Test fun blankQueryFindsNothing() {
        assertEquals(emptyList(), index.search("  "))
    }
}
