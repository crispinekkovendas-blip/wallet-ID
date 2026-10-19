package com.prenatal.app.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prenatal.app.data.EventKind

object P {
    val Bg = Color(0xFFFFF8F5)
    val Surface = Color(0xFFFFFFFF)
    val Surface2 = Color(0xFFFFF0EC)
    val Line = Color(0xFFF3E2DD)
    val Text = Color(0xFF2D2330)
    val Text2 = Color(0xFF6E6170)
    val Text3 = Color(0xFFA0939F)
    val Rose = Color(0xFFE0607E)
    val RoseDeep = Color(0xFFC94467)
    val Peach = Color(0xFFF4A58A)
    val Lavender = Color(0xFF8E7CC3)
    val Mint = Color(0xFF4FB59A)
    val Sky = Color(0xFF5B9BD5)
    val Amber = Color(0xFFE3A23B)
    val Bad = Color(0xFFD9534F)

    val RoseBrush = Brush.linearGradient(listOf(Color(0xFFF28CA5), Color(0xFFD9577A)))
    val HeroBrush = Brush.linearGradient(listOf(Color(0xFFFFD6DE), Color(0xFFFFE7D6), Color(0xFFEDE4FF)))

    fun kindColor(k: EventKind) = when (k) {
        EventKind.APPOINTMENT -> Rose
        EventKind.ULTRASOUND -> Lavender
        EventKind.LAB -> Sky
        EventKind.VACCINE -> Mint
        EventKind.OTHER -> Amber
    }
}

private val scheme = lightColorScheme(
    primary = P.Rose,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9E1),
    onPrimaryContainer = P.RoseDeep,
    secondary = P.Lavender,
    background = P.Bg,
    onBackground = P.Text,
    surface = P.Surface,
    onSurface = P.Text,
    surfaceVariant = P.Surface2,
    onSurfaceVariant = P.Text2,
    surfaceContainer = P.Surface,
    surfaceContainerHigh = P.Surface,
    surfaceContainerHighest = P.Surface2,
    surfaceContainerLow = P.Surface,
    outline = P.Line,
    outlineVariant = P.Line,
    error = P.Bad,
)

private val typography = Typography().let { t ->
    t.copy(
        headlineLarge = t.headlineLarge.copy(fontWeight = FontWeight.Bold, fontSize = 30.sp, letterSpacing = (-0.6).sp, color = P.Text),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 19.sp),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    )
}

@Composable
fun PrenatalTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = scheme,
        typography = typography,
        shapes = Shapes(
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(22.dp),
            extraLarge = RoundedCornerShape(28.dp),
        ),
        content = content,
    )
}
