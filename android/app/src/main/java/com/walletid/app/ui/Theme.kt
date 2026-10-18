package com.walletid.app.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object Palette {
    val Bg = Color(0xFF07090F)
    val Surface = Color(0xFF121725)
    val Surface2 = Color(0xFF181E2E)
    val Line = Color(0x12FFFFFF)
    val Line2 = Color(0x1FFFFFFF)
    val Text = Color(0xFFF2F4F8)
    val Text2 = Color(0xFFA3ABBD)
    val Text3 = Color(0xFF6B7387)
    val Accent = Color(0xFF5B8CFF)
    val Accent2 = Color(0xFF8A6BFF)
    val Gold = Color(0xFFD9B26A)
    val Warn = Color(0xFFFBBF24)
    val Bad = Color(0xFFF87171)

    val AccentBrush = Brush.linearGradient(listOf(Accent, Accent2))
    val GoldBrush = Brush.verticalGradient(listOf(Color(0xFFFFF3CF), Color(0xFFD9B26A), Color(0xFFB98D3F)))
}

private val colors = darkColorScheme(
    primary = Palette.Accent,
    onPrimary = Color.White,
    secondary = Palette.Accent2,
    background = Palette.Bg,
    onBackground = Palette.Text,
    surface = Palette.Surface,
    onSurface = Palette.Text,
    surfaceVariant = Palette.Surface2,
    onSurfaceVariant = Palette.Text2,
    surfaceContainerHigh = Palette.Surface2,
    surfaceContainerHighest = Palette.Surface2,
    surfaceContainer = Palette.Surface,
    outline = Palette.Line2,
    outlineVariant = Palette.Line,
    error = Palette.Bad,
)

private val typography = Typography().let { t ->
    t.copy(
        headlineLarge = t.headlineLarge.copy(fontWeight = FontWeight.Bold, fontSize = 32.sp, letterSpacing = (-0.8).sp),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
        labelSmall = t.labelSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp),
    )
}

val SectionLabel = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp, color = Palette.Text3)

@Composable
fun WalletTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = colors,
        typography = typography,
        shapes = Shapes(
            small = RoundedCornerShape(10.dp),
            medium = RoundedCornerShape(14.dp),
            large = RoundedCornerShape(18.dp),
            extraLarge = RoundedCornerShape(26.dp),
        ),
        content = content,
    )
}
