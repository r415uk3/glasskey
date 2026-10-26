// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.ios

import androidx.test.core.app.ApplicationProvider
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** the bundled CLDR data finds everyday words in all three languages */
@RunWith(RobolectricTestRunner::class)
class IosEmojiSearchAssetsTest {
    private val index = IosEmojiSearchIndex.load(ApplicationProvider.getApplicationContext(), listOf("ru", "kk"))

    @Test fun findsEverydayWords() {
        assertTrue("❤️" in index.search("сердце"), index.search("сердце").toString())
        assertTrue("🐱" in index.search("cat"), index.search("cat").toString())
        assertEquals("🇰🇿", index.search("қазақстан").first())
    }

    @Test fun otherKeyboardLanguagesAreSearchedToo() {
        val index = IosEmojiSearchIndex.load(ApplicationProvider.getApplicationContext(), listOf("de-DE", "uk"))
        assertTrue("🐱" in index.search("katze"), index.search("katze").toString())
        assertTrue("🐱" in index.search("кіт"), index.search("кіт").toString())
        assertTrue("🐱" in index.search("cat"), "English is always searched")
    }

    @Test fun onlyEnabledLanguagesAreLoaded() {
        val index = IosEmojiSearchIndex.load(ApplicationProvider.getApplicationContext(), listOf("ru"))
        assertEquals(emptyList(), index.search("katze"))
    }
}
