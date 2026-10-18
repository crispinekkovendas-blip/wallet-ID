package com.walletid.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.walletid.app.data.Doc
import com.walletid.app.data.ExpiryLevel
import com.walletid.app.data.formatBrMonth
import com.walletid.app.data.mask

const val CARD_RATIO = 1.586f

/** A document drawn as a premium wallet card. */
@Composable
fun IdCard(doc: Doc, modifier: Modifier = Modifier, reveal: Boolean = false) {
    val t = doc.type
    val c1 = Color(t.c1)
    val c2 = Color(t.c2)
    val ink = Color(t.ink)
    val shape = RoundedCornerShape(20.dp)

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .aspectRatio(CARD_RATIO)
            .shadow(18.dp, shape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(shape)
            .background(Brush.linearGradient(listOf(c1, c2), start = Offset.Zero, end = Offset.Infinite))
            .border(1.dp, Color.White.copy(alpha = 0.10f), shape),
    ) {
        val w = maxWidth
        val density = LocalDensity.current
        fun fs(fraction: Float, min: Float, max: Float): TextUnit =
            with(density) { (w * fraction).toSp().value.coerceIn(min, max).sp }

        Canvas(Modifier.fillMaxSize()) { drawCardArt() }

        val primary = doc.primaryField
        val value = doc.primaryValue
        val shown = when {
            value.isEmpty() -> "—"
            primary?.secret == true && !reveal -> mask(value)
            else -> value
        }
        val sub = when (t.key) {
            "other" -> doc["issuer"].ifEmpty { t.sub }
            "council" -> if (doc["council"].isNotEmpty()) t.name + doc["uf"].let { if (it.isEmpty()) "" else " · $it" } else t.name
            else -> t.name
        }
        val meta: Pair<String, String>? = when {
            doc["category"].isNotEmpty() -> "CAT" to doc["category"]
            doc["expires"].isNotEmpty() -> "VALID" to formatBrMonth(doc["expires"])
            doc["uf"].isNotEmpty() -> "UF" to doc["uf"]
            else -> null
        }
        val country = if (t.key == "passport" && doc["country"].isNotEmpty()) doc["country"].uppercase() else "BRASIL"
        val expiry = doc.expiry

        Column(Modifier.fillMaxSize().padding(horizontal = w * 0.07f, vertical = w * 0.065f)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(
                        doc.title,
                        style = inkStyle(t.gold, ink, fs(0.085f, 18f, 34f), (-0.4).sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        sub,
                        color = ink.copy(alpha = 0.78f),
                        fontSize = fs(0.034f, 10f, 13f),
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    BrazilFlag(Modifier.size(width = 18.dp, height = 13.dp))
                    Text(
                        country,
                        color = ink.copy(alpha = 0.85f),
                        fontSize = fs(0.028f, 9f, 11f),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.6.sp,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Chip(Modifier.width((w * 0.12f).coerceIn(30.dp, 46.dp)).aspectRatio(4f / 3f), gold = t.gold)
                Spacer(Modifier.weight(1f))
                if (expiry != null) {
                    Text(
                        if (expiry.level == ExpiryLevel.BAD) "EXPIRED" else "EXPIRING",
                        color = if (expiry.level == ExpiryLevel.BAD) Color(0xFFFECACA) else Color(0xFFFDE68A),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.35f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
            Text(
                shown,
                color = ink,
                fontFamily = FontFamily.Monospace,
                fontSize = fs(0.062f, 15f, 26f),
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = w * 0.035f, bottom = w * 0.025f),
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    CardLabel(if (t.key == "crlv") "OWNER" else "HOLDER", ink, fs(0.022f, 8f, 10f))
                    Text(
                        doc["holder"].ifEmpty { "—" }.uppercase(),
                        color = ink,
                        fontSize = fs(0.036f, 11f, 15f),
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.6.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (meta != null) {
                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 12.dp)) {
                        CardLabel(meta.first, ink, fs(0.022f, 8f, 10f))
                        Text(
                            meta.second,
                            color = ink,
                            fontFamily = FontFamily.Monospace,
                            fontSize = fs(0.036f, 11f, 15f),
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CardLabel(text: String, ink: Color, size: TextUnit) {
    Text(
        text,
        color = ink.copy(alpha = 0.6f),
        fontSize = size,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.4.sp,
        modifier = Modifier.padding(bottom = 2.dp),
    )
}

/** Light sheen, dark corner, guilloche rings and fine diagonal lines. */
private fun DrawScope.drawCardArt() {
    val w = size.width
    val h = size.height
    drawRect(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.18f), Color.Transparent), Offset(w, 0f), w * 0.95f))
    drawRect(Brush.radialGradient(listOf(Color.Black.copy(alpha = 0.32f), Color.Transparent), Offset(0f, h), w * 0.8f))

    val step = 15.dp.toPx()
    val ring = Stroke(width = 1.dp.toPx())
    val c1 = Offset(w * 1.1f, h * 1.2f)
    var r = step
    while (r < w * 1.6f) {
        drawCircle(Color.White.copy(alpha = 0.075f), r, c1, style = ring)
        r += step
    }
    val c2 = Offset(-w * 0.1f, -h * 0.3f)
    r = step * 1.3f
    while (r < w * 0.9f) {
        drawCircle(Color.White.copy(alpha = 0.045f), r, c2, style = ring)
        r += step * 1.3f
    }

    rotate(-30f) {
        val gap = 7.dp.toPx()
        var x = -h * 2
        while (x < w + h * 2) {
            drawLine(Color.White.copy(alpha = 0.045f), Offset(x, -h), Offset(x, h * 2), strokeWidth = 1f)
            x += gap
        }
    }

    drawRect(
        Brush.linearGradient(
            0.40f to Color.Transparent,
            0.48f to Color.White.copy(alpha = 0.12f),
            0.56f to Color.Transparent,
            start = Offset(0f, 0f),
            end = Offset(w, h * 0.6f),
        ),
    )
}

@Composable
private fun Chip(modifier: Modifier, gold: Boolean) {
    val base = if (gold) Color(0xFFE7C77F) else Color(0xFFE9E2CF)
    Canvas(modifier) {
        val corner = CornerRadius(size.width * 0.16f)
        drawRoundRect(
            Brush.linearGradient(listOf(base, base.copy(red = base.red * 0.86f, green = base.green * 0.84f, blue = base.blue * 0.8f))),
            cornerRadius = corner,
        )
        val line = Color.Black.copy(alpha = 0.26f)
        val sw = 1.dp.toPx()
        val w = size.width
        val h = size.height
        drawLine(line, Offset(0f, h * 0.33f), Offset(w * 0.33f, h * 0.33f), sw)
        drawLine(line, Offset(0f, h * 0.66f), Offset(w * 0.33f, h * 0.66f), sw)
        drawLine(line, Offset(w * 0.67f, h * 0.33f), Offset(w, h * 0.33f), sw)
        drawLine(line, Offset(w * 0.67f, h * 0.66f), Offset(w, h * 0.66f), sw)
        drawLine(line, Offset(w * 0.33f, 0f), Offset(w * 0.33f, h), sw)
        drawLine(line, Offset(w * 0.67f, 0f), Offset(w * 0.67f, h), sw)
        drawLine(line, Offset(w * 0.33f, h * 0.5f), Offset(w * 0.67f, h * 0.5f), sw)
    }
}

@Composable
fun BrazilFlag(modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRoundRect(Color(0xFF16A34A), size = Size(w, h), cornerRadius = CornerRadius(2.dp.toPx()))
        val inset = w * 0.12f
        val diamond = Path().apply {
            moveTo(w / 2, inset * 0.8f)
            lineTo(w - inset, h / 2)
            lineTo(w / 2, h - inset * 0.8f)
            lineTo(inset, h / 2)
            close()
        }
        drawPath(diamond, Color(0xFFFACC15))
        drawCircle(Color(0xFF1E40AF), h * 0.22f, Offset(w / 2, h / 2))
    }
}

/**
 * Lays a card out as if it were only [peek] tall while still drawing it in full,
 * so the next card in a column overlaps it (the wallet "stack").
 */
fun Modifier.peek(peek: Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val h = peek.roundToPx().coerceAtMost(placeable.height)
    layout(placeable.width, h) { placeable.place(0, 0) }
}

/** A small swatch for the type picker. */
@Composable
fun TypeSwatch(c1: Long, c2: Long, ink: Long, gold: Boolean, abbr: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(CARD_RATIO)
            .clip(RoundedCornerShape(10.dp))
            .background(Brush.linearGradient(listOf(Color(c1), Color(c2)), start = Offset.Zero, end = Offset.Infinite)),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                Brush.radialGradient(listOf(Color.White.copy(alpha = 0.2f), Color.Transparent), Offset(size.width, 0f), size.width * 0.9f),
            )
        }
        Text(
            abbr,
            style = inkStyle(gold, Color(ink), 15.sp, 0.sp),
            maxLines = 1,
            modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
        )
    }
}

/** Gold gradient lettering for premium documents, plain ink otherwise. */
private fun inkStyle(gold: Boolean, ink: Color, size: TextUnit, spacing: TextUnit): TextStyle =
    if (gold) {
        TextStyle(brush = Palette.GoldBrush, fontSize = size, fontWeight = FontWeight.ExtraBold, letterSpacing = spacing)
    } else {
        TextStyle(color = ink, fontSize = size, fontWeight = FontWeight.ExtraBold, letterSpacing = spacing)
    }
