// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.ios

import android.content.Context
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import helium314.keyboard.keyboard.KeyboardActionListener
import helium314.keyboard.latin.R
import helium314.keyboard.latin.common.ColorType
import helium314.keyboard.latin.common.Colors

/** Strip under the keys in iOS style. Sits above the gesture-navigation area by padding itself with the nav bar inset. */
class IosBottomBar(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private val globe: ImageButton get() = findViewById(R.id.ios_globe)
    private val basePaddingBottom = paddingBottom

    override fun onFinishInflate() {
        super.onFinishInflate()
        ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
            val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            view.updatePadding(bottom = basePaddingBottom + navBar.bottom)
            insets
        }
    }

    fun bind(listener: KeyboardActionListener, showPicker: () -> Unit) {
        val controller = GlobeController(
            onCode = { listener.onCodeInput(it, -1, -1, false) },
            showPicker = showPicker,
        )
        globe.setOnClickListener { controller.onTap() }
        globe.setOnLongClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            controller.onLongPress()
            true
        }
    }

    fun updateColors(colors: Colors) {
        colors.setColor(globe, ColorType.KEY_ICON)
        colors.setColor(findViewById<ImageButton>(R.id.ios_mic), ColorType.KEY_ICON)
    }
}
