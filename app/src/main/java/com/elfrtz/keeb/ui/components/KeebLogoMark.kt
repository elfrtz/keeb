package com.elfrtz.keeb.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.elfrtz.keeb.ui.theme.KeebColors

@Composable
fun KeebLogoMark(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(48.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 3.dp.toPx()
        val radius = 8.dp.toPx()

        drawRoundRect(
            color = KeebColors.AccentBlue,
            topLeft = Offset(w * 0.08f, h * 0.12f),
            size = Size(w * 0.84f, h * 0.76f),
            cornerRadius = CornerRadius(radius, radius),
            style = Stroke(width = stroke)
        )

        val keyPath = Path().apply {
            moveTo(w * 0.28f, h * 0.38f)
            lineTo(w * 0.72f, h * 0.38f)
            lineTo(w * 0.72f, h * 0.62f)
            lineTo(w * 0.52f, h * 0.62f)
            lineTo(w * 0.52f, h * 0.78f)
            lineTo(w * 0.38f, h * 0.78f)
            lineTo(w * 0.38f, h * 0.62f)
            lineTo(w * 0.28f, h * 0.62f)
            close()
        }
        drawPath(keyPath, KeebColors.AccentBlue)
    }
}
