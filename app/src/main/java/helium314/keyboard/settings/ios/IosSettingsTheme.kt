// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.ios

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class IosSettingsColors(
    val background: Color,
    val groupFill: Color,
    val text: Color,
    val secondaryText: Color,
    val separator: Color,
    val tint: Color,
    val switchOn: Color,
    val switchOff: Color,
    /** Background of a modal sheet / card (iOS grouped sheet colour). */
    val sheet: Color,
)

object IosSettingsTheme {
    private val dark = IosSettingsColors(
        background = Color(0xFF05060A),
        groupFill = Color(0x14FFFFFF), // rgba(255,255,255,.08)
        text = Color.White,
        secondaryText = Color(0x99EBEBF5), // rgba(235,235,245,.6)
        separator = Color(0x24FFFFFF), // rgba(255,255,255,.14)
        tint = Color(0xFF0A84FF),
        switchOn = Color(0xFF30D158),
        switchOff = Color(0x5C787880), // rgba(120,120,128,.36)
        sheet = Color(0xFF1C1C1E),
    )
    private val light = IosSettingsColors(
        background = Color(0xFFF2F2F7),
        groupFill = Color(0x8CFFFFFF), // rgba(255,255,255,.55)
        text = Color.Black,
        secondaryText = Color(0x993C3C43), // rgba(60,60,67,.6)
        separator = Color(0x2E3C3C43), // rgba(60,60,67,.18)
        tint = Color(0xFF0A84FF),
        switchOn = Color(0xFF34C759),
        switchOff = Color(0x29787880), // rgba(120,120,128,.16)
        sheet = Color(0xFFF2F2F7),
    )

    fun colors(dark: Boolean): IosSettingsColors = if (dark) this.dark else light
}

val LocalIosColors = staticCompositionLocalOf { IosSettingsTheme.colors(dark = true) }
val LocalIosDark = staticCompositionLocalOf { true }

@Composable
fun IosSettingsTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalIosColors provides IosSettingsTheme.colors(dark),
        LocalIosDark provides dark,
        content = content
    )
}
