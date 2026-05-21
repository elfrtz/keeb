package com.elfrtz.keeb.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.elfrtz.keeb.ui.theme.KeebColors
import com.elfrtz.keeb.ui.theme.KeebRadius
import com.elfrtz.keeb.ui.theme.KeebType

@Composable
fun TestnetBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(KeebRadius.full))
            .background(KeebColors.AccentAmberMuted)
            .border(0.5.dp, KeebColors.AccentAmber.copy(alpha = 0.4f), RoundedCornerShape(KeebRadius.full))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text("TESTNET", style = KeebType.label, color = KeebColors.AccentAmber)
    }
}
