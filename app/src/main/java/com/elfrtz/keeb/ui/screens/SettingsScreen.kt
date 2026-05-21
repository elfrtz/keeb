package com.elfrtz.keeb.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.elfrtz.keeb.BuildConfig
import com.elfrtz.keeb.R
import com.elfrtz.keeb.keyboard.KeyboardSettings
import com.elfrtz.keeb.ui.theme.KeebColors
import com.elfrtz.keeb.ui.theme.KeebRadius
import com.elfrtz.keeb.ui.theme.KeebSpacing
import com.elfrtz.keeb.ui.theme.KeebType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    keyHeight: KeyboardSettings.KeyHeight,
    vibrationEnabled: Boolean,
    soundEnabled: Boolean,
    onBack: () -> Unit,
    onKeyHeightSelect: (KeyboardSettings.KeyHeight) -> Unit,
    onVibrationChange: (Boolean) -> Unit,
    onSoundChange: (Boolean) -> Unit,
    onResetDefaults: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            containerColor = KeebColors.BgElevated,
            title = {
                Text("Reset to defaults?", style = KeebType.bodyMedium, color = KeebColors.TextPrimary)
            },
            text = {
                Text(
                    "Key height, vibration, and sound will be restored.",
                    style = KeebType.bodyRegular,
                    color = KeebColors.TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showResetDialog = false
                    onResetDefaults()
                }) {
                    Text("Reset", color = KeebColors.AccentRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = KeebColors.TextSecondary)
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(KeebColors.BgPrimary)
            .padding(KeebSpacing.xl)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = KeebColors.TextSecondary
                )
            }
            Text(
                stringResource(R.string.settings_title),
                style = KeebType.displayMedium,
                color = KeebColors.TextPrimary,
                modifier = Modifier.padding(start = KeebSpacing.sm)
            )
        }

        Spacer(Modifier.height(KeebSpacing.xl))

        Text(
            stringResource(R.string.settings_height_label),
            style = KeebType.label,
            color = KeebColors.TextMuted
        )
        Spacer(Modifier.height(KeebSpacing.sm))
        KeyHeightSelector(selected = keyHeight, onSelect = onKeyHeightSelect)

        Spacer(Modifier.height(KeebSpacing.xl))

        SettingsToggleRow(
            label = stringResource(R.string.settings_vibration),
            checked = vibrationEnabled,
            onCheckedChange = onVibrationChange
        )
        SettingsToggleRow(
            label = stringResource(R.string.settings_sound),
            checked = soundEnabled,
            onCheckedChange = onSoundChange
        )

        Spacer(Modifier.weight(1f))

        Text(
            "Keeb v${BuildConfig.VERSION_NAME}",
            style = KeebType.label,
            color = KeebColors.TextMuted,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(KeebSpacing.sm))
        TextButton(
            onClick = { showResetDialog = true },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Reset to defaults", color = KeebColors.AccentRed, style = KeebType.bodyMedium)
        }
    }
}

@Composable
fun KeyHeightSelector(
    selected: KeyboardSettings.KeyHeight,
    onSelect: (KeyboardSettings.KeyHeight) -> Unit
) {
    val options = KeyboardSettings.KeyHeight.entries
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KeebRadius.md))
            .background(KeebColors.BgElevated)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(KeebRadius.sm))
                    .background(if (isSelected) KeebColors.AccentBlue else Color.Transparent)
                    .clickable { onSelect(option) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    option.label,
                    style = KeebType.bodyMedium,
                    color = if (isSelected) Color.White else KeebColors.TextSecondary
                )
            }
        }
    }
}

@Composable
fun SettingsToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = KeebSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = KeebType.bodyRegular,
            color = KeebColors.TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = KeebColors.AccentBlue,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = KeebColors.BgKeySpecial,
                uncheckedBorderColor = KeebColors.BorderDefault
            )
        )
    }
}
