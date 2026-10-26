// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard

import androidx.test.core.app.ApplicationProvider
import android.content.Context
import helium314.keyboard.latin.BuildConfig
import helium314.keyboard.latin.R
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class RebrandTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test fun applicationIdIsOurs() {
        assertTrue(BuildConfig.APPLICATION_ID.startsWith("io.github.r415uk3.glasskey"), BuildConfig.APPLICATION_ID)
    }

    @Test fun authoritiesAreNotHeliboards() {
        for (id in listOf(R.string.clipboard_provider_authority, R.string.gesture_data_provider_authority)) {
            val authority = context.getString(id)
            assertFalse(authority.startsWith("helium314.keyboard"), authority)
        }
    }

    @Test fun imeNameIsGlasskey() {
        assertTrue(context.getString(R.string.english_ime_name).startsWith("Glasskey"))
    }
}
