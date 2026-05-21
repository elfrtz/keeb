package com.elfrtz.keeb.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.elfrtz.keeb.ui.theme.KeebColors
import com.elfrtz.keeb.ui.theme.KeebType

@Composable
fun DangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showIcon: Boolean = true
) {
    TextButton(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        if (showIcon) {
            Icon(
                Icons.AutoMirrored.Outlined.Logout,
                contentDescription = null,
                tint = KeebColors.AccentRed,
                modifier = Modifier
            )
        }
        Text(text, style = KeebType.bodyMedium, color = KeebColors.AccentRed)
    }
}
