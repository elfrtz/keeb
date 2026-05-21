package com.elfrtz.keeb.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.elfrtz.keeb.ui.theme.KeebColors
import com.elfrtz.keeb.ui.theme.KeebRadius
import com.elfrtz.keeb.ui.theme.KeebType

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 52.dp
) {
    val interactionSource = MutableInteractionSource()
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "secondaryButtonScale"
    )

    OutlinedButton(
        onClick = onClick,
        interactionSource = interactionSource,
        border = BorderStroke(0.5.dp, KeebColors.BorderDefault),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = KeebColors.TextSecondary),
        shape = RoundedCornerShape(KeebRadius.full),
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .scale(scale)
    ) {
        Text(text, style = KeebType.bodyMedium)
    }
}
