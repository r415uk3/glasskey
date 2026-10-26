// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.ios.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import helium314.keyboard.latin.R

const val IosDeleteTag = "ios_delete"

private val iosRed = Color(0xFFFF3B30)

/** Red minus in front of a row in edit mode; [onClick] reveals or hides [IosConfirmDeleteButton]. */
@Composable
fun IosDeleteToggle(name: String, onClick: () -> Unit) {
    val deleteText = stringResource(R.string.ios_delete)
    Box(
        Modifier
            .testTag(IosDeleteTag)
            .size(22.dp)
            .background(iosRed, CircleShape)
            .clickable(remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "$deleteText $name" },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(width = 10.dp, height = 2.dp).background(Color.White, RoundedCornerShape(1.dp)))
    }
}

/** Red «Удалить» capsule at the end of a row whose delete toggle was tapped. */
@Composable
fun IosConfirmDeleteButton(onClick: () -> Unit) {
    Box(
        Modifier
            .background(iosRed, RoundedCornerShape(percent = 50))
            .clickable(remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        BasicText(stringResource(R.string.ios_delete), style = TextStyle(color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold))
    }
}
