// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.ios

import android.content.Context
import android.os.Build
import android.view.Window
import android.view.WindowManager

/** iOS glass: the system blurs what is behind the keyboard window (Android 12+, when the device allows it). */
object IosBlur {
    @JvmStatic
    fun isAvailable(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            && context.getSystemService(WindowManager::class.java)?.isCrossWindowBlurEnabled == true

    @JvmStatic
    fun apply(window: Window?, context: Context) {
        if (window == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val radius = if (isAvailable(context)) (30 * context.resources.displayMetrics.density).toInt() else 0
        window.setBackgroundBlurRadius(radius)
    }
}
