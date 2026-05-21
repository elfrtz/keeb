package com.elfrtz.keeb.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.elfrtz.keeb.R
import com.elfrtz.keeb.ui.components.AddressDisplay
import com.elfrtz.keeb.ui.components.DangerButton
import com.elfrtz.keeb.ui.components.SecondaryButton
import com.elfrtz.keeb.ui.components.TestnetBadge
import com.elfrtz.keeb.ui.sheets.LinkWalletBottomSheet
import com.elfrtz.keeb.ui.theme.KeebColors
import com.elfrtz.keeb.ui.theme.KeebRadius
import com.elfrtz.keeb.ui.theme.KeebSpacing
import com.elfrtz.keeb.ui.theme.KeebType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    isReady: Boolean,
    address: String?,
    onBack: () -> Unit,
    onSaveKey: (String) -> Unit,
    onDisconnect: () -> Unit,
    onOpenMetaMask: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showLinkSheet by remember { mutableStateOf(false) }
    var showDisconnectDialog by remember { mutableStateOf(false) }

    if (showLinkSheet) {
        LinkWalletBottomSheet(
            onDismiss = { showLinkSheet = false },
            onSave = { key ->
                showLinkSheet = false
                onSaveKey(key)
            }
        )
    }

    if (showDisconnectDialog) {
        AlertDialog(
            onDismissRequest = { showDisconnectDialog = false },
            containerColor = KeebColors.BgElevated,
            title = {
                Text("Disconnect wallet?", style = KeebType.bodyMedium, color = KeebColors.TextPrimary)
            },
            text = {
                Text(
                    stringResource(R.string.wallet_disconnected),
                    style = KeebType.bodyRegular,
                    color = KeebColors.TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDisconnectDialog = false
                    onDisconnect()
                }) {
                    Text("Disconnect", color = KeebColors.AccentRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectDialog = false }) {
                    Text("Cancel", color = KeebColors.TextSecondary)
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(KeebColors.BgPrimary)
            .verticalScroll(rememberScrollState())
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
                stringResource(R.string.wallet_title),
                style = KeebType.displayMedium,
                color = KeebColors.TextPrimary,
                modifier = Modifier.padding(start = KeebSpacing.sm)
            )
            Spacer(Modifier.weight(1f))
            TestnetBadge()
        }

        Spacer(Modifier.height(KeebSpacing.xl))

        Text(
            text = if (isReady) {
                stringResource(R.string.wallet_state_ready)
            } else {
                stringResource(R.string.wallet_state_not_ready)
            },
            style = KeebType.displayMedium,
            color = if (isReady) KeebColors.AccentGreen else KeebColors.TextSecondary
        )

        Spacer(Modifier.height(KeebSpacing.sm))

        Text(
            text = if (isReady) {
                stringResource(R.string.wallet_hint_ready)
            } else {
                stringResource(R.string.wallet_hint_setup)
            },
            style = KeebType.bodyRegular,
            color = KeebColors.TextSecondary
        )

        Spacer(Modifier.height(KeebSpacing.lg))

        if (address != null) {
            AddressDisplay(fullAddress = address)
            Spacer(Modifier.height(KeebSpacing.lg))
        }

        OutlinedButton(
            onClick = { showLinkSheet = true },
            border = androidx.compose.foundation.BorderStroke(0.5.dp, KeebColors.BorderDefault),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = KeebColors.TextSecondary),
            shape = RoundedCornerShape(KeebRadius.full),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(
                if (isReady) {
                    stringResource(R.string.wallet_change_key)
                } else {
                    stringResource(R.string.wallet_setup_button)
                },
                style = KeebType.bodyMedium
            )
        }

        if (!isReady) {
            Spacer(Modifier.height(KeebSpacing.sm))
            SecondaryButton(
                text = stringResource(R.string.wallet_open_metamask),
                onClick = onOpenMetaMask
            )
        }

        if (isReady) {
            Spacer(Modifier.height(KeebSpacing.lg))
            DangerButton(
                text = stringResource(R.string.wallet_disconnect),
                onClick = { showDisconnectDialog = true }
            )
        }

        Spacer(Modifier.height(KeebSpacing.xxl))

        Text(
            stringResource(R.string.wallet_footer),
            style = KeebType.label,
            color = KeebColors.TextMuted
        )
    }
}
