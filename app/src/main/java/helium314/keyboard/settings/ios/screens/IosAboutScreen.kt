// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.ios.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import helium314.keyboard.latin.BuildConfig
import helium314.keyboard.latin.R
import helium314.keyboard.settings.ios.IosRoute
import helium314.keyboard.settings.ios.IosRow
import helium314.keyboard.settings.ios.IosScreen
import helium314.keyboard.settings.ios.LocalIosColors
import helium314.keyboard.settings.ios.iosGroup

const val IOS_SOURCE_URL = "https://github.com/r415uk3/glasskey"

/** App icon and name, version, source link and licences. */
@Composable
fun IosAboutScreen(onBack: () -> Unit, navigate: (String) -> Unit) {
    val uriHandler = LocalUriHandler.current
    val tint = LocalIosColors.current.tint
    val version = stringResource(R.string.ios_version)
    val source = stringResource(R.string.ios_source_code)
    val licenses = stringResource(R.string.ios_licenses)

    IosScreen(stringResource(R.string.ios_about), onBack) {
        item { AppHeader() }
        iosGroup(rows = listOf { IosRow(version, BuildConfig.VERSION_NAME, chevron = false) })
        iosGroup(rows = listOf(
            { IosRow(source, titleColor = tint, chevron = false, onClick = { uriHandler.openUri(IOS_SOURCE_URL) }) },
            { IosRow(licenses, onClick = { navigate(IosRoute.Licenses) }) },
        ))
    }
}

@Composable
private fun AppHeader() {
    val ctx = LocalContext.current
    val colors = LocalIosColors.current
    val px = with(LocalDensity.current) { 88.dp.roundToPx() }
    val icon = remember(px) {
        runCatching { ContextCompat.getDrawable(ctx, R.mipmap.ic_launcher_round)?.toBitmap(px, px)?.asImageBitmap() }.getOrNull()
    }
    Column(
        Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (icon != null)
            Image(icon, null, Modifier.size(88.dp).clip(RoundedCornerShape(percent = 50)))
        BasicText(
            stringResource(R.string.ios_app_name),
            style = TextStyle(color = colors.text, fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
        )
    }
}

private class License(val name: String, @StringRes val nameRes: Int?, val license: String, @StringRes val licenseRes: Int?, val url: String)

private val ownLicenses = listOf(
    License("Glasskey", null, "GPL-3.0-only", null, IOS_SOURCE_URL),
    License("HeliBoard", null, "GPL-3.0-only", null, "https://github.com/Helium314/HeliBoard"),
)

// docs/third-party.md
private val thirdPartyLicenses = listOf(
    License("Inter 4.1", null, "SIL OFL 1.1", null, "https://github.com/rsms/inter"),
    License("Golos Text", null, "SIL OFL 1.1", null, "https://github.com/google/fonts/tree/main/ofl/golostext"),
    License("Onest", null, "SIL OFL 1.1", null, "https://github.com/google/fonts/tree/main/ofl/onest"),
    License("Noto Sans", null, "SIL OFL 1.1", null, "https://github.com/google/fonts/tree/main/ofl/notosans"),
    License("", R.string.ios_emoji_names, "Unicode License v3", null, "https://github.com/unicode-org/cldr-json"),
    License("", R.string.ios_kazakh_dictionary, "", R.string.ios_license_as_in_repo, "https://codeberg.org/Helium314/aosp-dictionaries"),
    License("AndroidHiddenApiBypass", null, "Apache-2.0", null, "https://github.com/LSPosed/AndroidHiddenApiBypass"),
    License("Backdrop (AndroidLiquidGlass) 1.0.1", null, "Apache-2.0", null, "https://github.com/Kyant0/AndroidLiquidGlass"),
    License("Liquid Glass catalog (AndroidLiquidGlass)", null, "Apache-2.0", null, "https://github.com/Kyant0/AndroidLiquidGlass/tree/65ab177e90e5c1d8c62e70cf7755841982da65f6/catalog"),
)

/** Glasskey's own licence and those of the bundled third-party parts; a tap opens a part's source. */
@Composable
fun IosLicensesScreen(onBack: () -> Unit) {
    val footer = stringResource(R.string.ios_licenses_footer)
    val thirdParty = stringResource(R.string.ios_third_party)
    IosScreen(stringResource(R.string.ios_licenses), onBack) {
        iosGroup(footer = footer, rows = ownLicenses.map { { LicenseRow(it) } })
        iosGroup(header = thirdParty, rows = thirdPartyLicenses.map { { LicenseRow(it) } })
    }
}

@Composable
private fun LicenseRow(license: License) {
    val uriHandler = LocalUriHandler.current
    IosRow(
        license.nameRes?.let { stringResource(it) } ?: license.name,
        subtitle = license.licenseRes?.let { stringResource(it) } ?: license.license,
        chevron = false,
        onClick = { uriHandler.openUri(license.url) },
    )
}
