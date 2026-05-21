package com.elfrtz.keeb.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.elfrtz.keeb.ui.components.KeebLogoMark
import com.elfrtz.keeb.ui.components.PrimaryButton
import com.elfrtz.keeb.ui.components.SecondaryButton
import com.elfrtz.keeb.ui.components.StatusCard
import com.elfrtz.keeb.ui.theme.KeebColors
import com.elfrtz.keeb.ui.theme.KeebSpacing
import com.elfrtz.keeb.ui.theme.KeebType

@Composable
fun HomeScreen(
    keyboardEnabled: Boolean,
    walletConnected: Boolean,
    walletStatusText: String,
    onEnableKeyboard: () -> Unit,
    onSwitchKeyboard: () -> Unit,
    onOpenWallet: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(KeebColors.BgPrimary)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = KeebSpacing.xl, vertical = KeebSpacing.xxl)
    ) {
        KeebLogoMark()
        Spacer(Modifier.height(KeebSpacing.lg))
        Text("Keeb", style = KeebType.displayLarge, color = KeebColors.TextPrimary)
        Text(
            "Send USDC from any chat app",
            style = KeebType.bodyRegular,
            color = KeebColors.TextSecondary
        )
        Spacer(Modifier.height(KeebSpacing.xl))

        StatusCard(
            label = "KEYBOARD",
            value = if (keyboardEnabled) "Enabled" else "Not enabled",
            isActive = keyboardEnabled,
            icon = Icons.Outlined.Keyboard
        )
        Spacer(Modifier.height(KeebSpacing.sm))
        StatusCard(
            label = "WALLET",
            value = walletStatusText,
            isActive = walletConnected,
            icon = Icons.Outlined.AccountBalanceWallet
        )
        Spacer(Modifier.height(KeebSpacing.xl))

        if (!keyboardEnabled) {
            SecondaryButton(
                text = "1.  Enable Keeb in Settings",
                onClick = onEnableKeyboard
            )
            Spacer(Modifier.height(KeebSpacing.sm))
        }
        PrimaryButton(
            text = "2.  Switch to Keeb",
            onClick = onSwitchKeyboard
        )

        Spacer(Modifier.height(KeebSpacing.xxl))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            BottomNavItem(
                label = "Wallet",
                icon = Icons.Outlined.AccountBalanceWallet,
                selected = true,
                onClick = onOpenWallet
            )
            BottomNavItem(
                label = "Settings",
                icon = Icons.Outlined.Settings,
                selected = false,
                onClick = onOpenSettings
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .height(52.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = KeebSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) KeebColors.AccentBlue else KeebColors.TextMuted,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style = KeebType.label,
            color = if (selected) KeebColors.AccentBlue else KeebColors.TextMuted
        )
        if (selected) {
            Spacer(Modifier.height(4.dp))
            BoxIndicator()
        }
    }
}

@Composable
private fun BoxIndicator() {
    Box(
        modifier = Modifier
            .size(width = 24.dp, height = 2.dp)
            .background(KeebColors.AccentBlue, androidx.compose.foundation.shape.RoundedCornerShape(1.dp))
    )
}
