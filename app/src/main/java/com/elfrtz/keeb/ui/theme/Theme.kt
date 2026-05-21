package com.elfrtz.keeb.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elfrtz.keeb.R

object KeebColors {
    val BgPrimary = Color(0xFF0D1117)
    val BgSecondary = Color(0xFF161B22)
    val BgElevated = Color(0xFF1C2128)
    val BgKey = Color(0xFF1C2128)
    val BgKeyPressed = Color(0xFF2D333B)
    val BgKeySpecial = Color(0xFF2D333B)

    val AccentBlue = Color(0xFF3B6EF5)
    val AccentBlueMuted = Color(0x263B6EF5)
    val AccentGreen = Color(0xFF00D4A1)
    val AccentGreenMuted = Color(0x2600D4A1)
    val AccentRed = Color(0xFFEF4444)
    val AccentRedMuted = Color(0x26EF4444)
    val AccentAmber = Color(0xFFF59E0B)
    val AccentAmberMuted = Color(0x26F59E0B)

    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFF8B949E)
    val TextMuted = Color(0xFF484F58)
    val TextMono = Color(0xFF79C0FF)

    val BorderDefault = Color(0x14FFFFFF)
    val BorderAccent = Color(0x663B6EF5)
    val BorderKey = Color(0x14FFFFFF)
    val BorderKeyBottom = Color(0x66000000)

    val ShiftActive = Color(0xFF3B6EF5)
    val ShiftCaps = Color(0xFF00D4A1)
}

object KeebSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

object KeebRadius {
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val full = 999.dp
}

val SyneFontFamily = FontFamily(Font(R.font.syne_bold, FontWeight.Bold))
val JetBrainsMonoFontFamily = FontFamily(Font(R.font.jetbrains_mono_regular))
val InterFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium)
)

object KeebType {
    val displayLarge = TextStyle(
        fontFamily = SyneFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp
    )
    val displayMedium = TextStyle(
        fontFamily = SyneFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp
    )
    val bodyRegular = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    )
    val bodyMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    )
    val mono = TextStyle(
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp
    )
    val label = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.06.sp
    )
    val keyLabel = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    )
}

fun Modifier.glassCard(): Modifier = this
    .clip(RoundedCornerShape(KeebRadius.lg))
    .then(Modifier)
