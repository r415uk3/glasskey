// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.ios

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import android.view.View
import android.view.WindowManager

/**
 * iOS glass: blur only what is behind the keyboard panel, using the platform's background blur drawable
 * (the one SystemUI uses). It is not public API, so everything here is best effort: null means "no blur".
 */
object IosBlur {
    private const val TAG = "GlasskeyBlur"

    @JvmStatic
    fun isAvailable(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            && context.getSystemService(WindowManager::class.java)?.isCrossWindowBlurEnabled == true

    /** a drawable that blurs behind [view]'s bounds, or null if the platform refuses */
    @JvmStatic
    fun createPanelBlur(view: View, color: Int, topCornerRadius: Float): Drawable? {
        if (!isAvailable(view.context)) { Log.i(TAG, "cross-window blur not available (sdk ${Build.VERSION.SDK_INT})"); return null }
        return try {
            val viewRoot = view.rootView.parent ?: return null // ViewRootImpl
            val drawable = viewRoot.javaClass.getMethod("createBackgroundBlurDrawable").invoke(viewRoot) as Drawable
            val cls = drawable.javaClass
            cls.getMethod("setBlurRadius", Int::class.javaPrimitiveType).invoke(drawable, (30 * view.resources.displayMetrics.density).toInt())
            cls.getMethod("setColor", Int::class.javaPrimitiveType).invoke(drawable, color)
            cls.getMethod("setCornerRadius", Float::class.javaPrimitiveType, Float::class.javaPrimitiveType, Float::class.javaPrimitiveType, Float::class.javaPrimitiveType)
                .invoke(drawable, topCornerRadius, topCornerRadius, 0f, 0f)
            Log.i(TAG, "panel blur created: ${cls.name}")
            drawable
        } catch (e: Throwable) {
            Log.w(TAG, "panel blur not available", e)
            null
        }
    }
}
