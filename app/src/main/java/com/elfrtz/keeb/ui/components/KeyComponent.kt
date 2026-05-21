package com.elfrtz.keeb.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.elfrtz.keeb.ui.theme.KeebColors
import com.elfrtz.keeb.ui.theme.KeebRadius
import com.elfrtz.keeb.ui.theme.KeebType

@Composable
fun Key(
    label: String,
    modifier: Modifier = Modifier,
    height: Dp = 44.dp,
    isSpecial: Boolean = false,
    isDestructive: Boolean = false,
    isShiftActive: Boolean = false,
    isCapsLock: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onPressEnd: (() -> Unit)? = null
) {
    var pressed by remember { mutableStateOf(false) }
    val bgColor = when {
        isDestructive -> KeebColors.AccentRedMuted
        isShiftActive || isCapsLock -> when {
            isCapsLock -> KeebColors.AccentGreenMuted
            else -> KeebColors.AccentBlueMuted
        }
        isSpecial -> KeebColors.BgKeySpecial
        else -> KeebColors.BgKey
    }

    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(KeebRadius.sm))
            .background(if (pressed) KeebColors.BgKeyPressed else bgColor)
            .border(
                width = 0.5.dp,
                color = when {
                    isDestructive -> KeebColors.AccentRed.copy(alpha = 0.2f)
                    else -> KeebColors.BorderKey
                },
                shape = RoundedCornerShape(KeebRadius.sm)
            )
            .pointerInput(onClick, onLongClick, onPressEnd) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                        onPressEnd?.invoke()
                    },
                    onTap = { onClick() },
                    onLongPress = { onLongClick?.invoke() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = KeebType.keyLabel,
            color = when {
                isDestructive -> KeebColors.AccentRed
                isShiftActive || isCapsLock -> KeebColors.AccentBlue
                isSpecial -> KeebColors.TextSecondary
                else -> KeebColors.TextPrimary
            }
        )
    }
}
