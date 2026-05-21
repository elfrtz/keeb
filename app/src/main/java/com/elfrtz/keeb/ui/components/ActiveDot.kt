package com.elfrtz.keeb.ui.components

import android.provider.Settings
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.elfrtz.keeb.ui.theme.KeebColors

@Composable
fun ActiveDot(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val reduceMotion = try {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) == 0f
    } catch (_: Exception) {
        false
    }

    val alpha = if (!reduceMotion) {
        val infiniteTransition = rememberInfiniteTransition(label = "activeDot")
        val animatedAlpha by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
            label = "activeDotAlpha"
        )
        animatedAlpha
    } else {
        1f
    }

    Box(
        modifier = modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(KeebColors.AccentGreen.copy(alpha = alpha))
    )
}
