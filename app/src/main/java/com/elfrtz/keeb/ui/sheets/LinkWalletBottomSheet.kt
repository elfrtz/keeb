package com.elfrtz.keeb.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.elfrtz.keeb.ui.theme.KeebColors
import com.elfrtz.keeb.ui.theme.KeebRadius
import com.elfrtz.keeb.ui.theme.KeebSpacing
import com.elfrtz.keeb.ui.theme.KeebType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkWalletBottomSheet(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var privateKey by remember { mutableStateOf("") }
    var keyVisible by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = KeebColors.BgSecondary,
        dragHandle = {
            Box(
                Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(KeebColors.BorderDefault)
            )
        },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(KeebSpacing.lg)
        ) {
            Text(
                "Link your Base Sepolia wallet",
                style = KeebType.displayMedium,
                color = KeebColors.TextPrimary
            )

            listOf(
                "Switch to Base Sepolia Testnet in MetaMask",
                "Account → ⋮ → Account details → Show private key",
                "Paste that key below"
            ).forEachIndexed { i, step ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(KeebSpacing.md),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(KeebColors.AccentBlueMuted),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${i + 1}", style = KeebType.bodyMedium, color = KeebColors.AccentBlue)
                    }
                    Text(
                        step,
                        style = KeebType.bodyRegular,
                        color = KeebColors.TextSecondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            OutlinedTextField(
                value = privateKey,
                onValueChange = { privateKey = it },
                placeholder = {
                    Text(
                        "Private key (with or without 0x)",
                        color = KeebColors.TextMuted
                    )
                },
                visualTransformation = if (keyVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(onClick = { keyVisible = !keyVisible }) {
                        Icon(
                            if (keyVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = "Toggle visibility",
                            tint = KeebColors.TextSecondary
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = KeebColors.AccentBlue,
                    unfocusedBorderColor = KeebColors.BorderDefault,
                    focusedTextColor = KeebColors.TextPrimary,
                    unfocusedTextColor = KeebColors.TextPrimary,
                    cursorColor = KeebColors.AccentBlue
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(KeebRadius.md)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = KeebColors.TextMuted,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    "Stored locally on this device only",
                    style = KeebType.label,
                    color = KeebColors.TextMuted
                )
            }

            Button(
                onClick = { onSave(privateKey) },
                colors = ButtonDefaults.buttonColors(containerColor = KeebColors.AccentBlue),
                shape = RoundedCornerShape(KeebRadius.full),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("Save & enable sends", color = Color.White, style = KeebType.bodyMedium)
            }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel", color = KeebColors.TextSecondary)
            }
        }
    }
}
