package com.elfrtz.keeb.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.elfrtz.keeb.ui.theme.KeebColors
import com.elfrtz.keeb.ui.theme.KeebRadius
import com.elfrtz.keeb.ui.theme.KeebSpacing
import com.elfrtz.keeb.ui.theme.KeebType
import kotlinx.coroutines.delay

@Composable
fun AddressDisplay(fullAddress: String, modifier: Modifier = Modifier) {
    val truncated = "${fullAddress.take(6)}...${fullAddress.takeLast(4)}"
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(1500)
            copied = false
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KeebRadius.md))
            .background(KeebColors.BgElevated)
            .padding(KeebSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("ADDRESS", style = KeebType.label, color = KeebColors.TextMuted)
            Text(truncated, style = KeebType.mono, color = KeebColors.TextMono)
        }
        IconButton(
            onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("wallet", fullAddress))
                copied = true
            }
        ) {
            Icon(
                imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                contentDescription = "Copy wallet address",
                tint = if (copied) KeebColors.AccentGreen else KeebColors.TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
