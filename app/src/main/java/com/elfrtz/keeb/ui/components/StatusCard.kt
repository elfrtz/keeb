package com.elfrtz.keeb.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.elfrtz.keeb.ui.theme.KeebColors
import com.elfrtz.keeb.ui.theme.KeebRadius
import com.elfrtz.keeb.ui.theme.KeebSpacing
import com.elfrtz.keeb.ui.theme.KeebType

@Composable
fun StatusCard(
    label: String,
    value: String,
    isActive: Boolean,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KeebRadius.lg))
            .background(KeebColors.BgElevated)
            .border(0.5.dp, KeebColors.BorderDefault, RoundedCornerShape(KeebRadius.lg))
            .padding(KeebSpacing.lg)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(KeebSpacing.sm)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) KeebColors.AccentGreen else KeebColors.TextMuted,
                modifier = Modifier.size(18.dp)
            )
            Column {
                Text(label, style = KeebType.label, color = KeebColors.TextMuted)
                Text(value, style = KeebType.bodyMedium, color = KeebColors.TextPrimary)
            }
            Spacer(Modifier.weight(1f))
            if (isActive) ActiveDot()
        }
    }
}
