package com.walletid.app.ui

import android.app.Activity
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FlipCameraAndroid
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.walletid.app.data.Doc
import com.walletid.app.data.QrModules
import kotlin.math.floor

private val CardShape = RoundedCornerShape(18.dp)

/** A card in the wallet: the uploaded photo itself, or the designed template if there's no photo. */
@Composable
fun PhotoCard(doc: Doc, image: ImageBitmap?, modifier: Modifier = Modifier) {
    if (image == null) {
        IdCard(doc, modifier)
        return
    }
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(CARD_RATIO)
            .shadow(18.dp, CardShape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(CardShape)
            .background(Palette.Surface2)
            .border(1.dp, Color.White.copy(alpha = 0.10f), CardShape),
    ) {
        Image(image, doc.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    }
}

/**
 * The expanded card. Tap to flip it over (3D) and see the back photo.
 * Sized to the front photo's own proportions so nothing is cropped.
 */
@Composable
fun FlipCard(
    doc: Doc,
    front: ImageBitmap?,
    back: ImageBitmap?,
    flipped: Boolean,
    onFlip: () -> Unit,
    modifier: Modifier = Modifier,
    frontModifier: Modifier = Modifier,
) {
    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(650, easing = FastOutSlowInEasing),
        label = "flip",
    )
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val ratio = front?.let { it.width.toFloat() / it.height } ?: CARD_RATIO
        val w = if (maxWidth / ratio <= maxHeight) maxWidth else maxHeight * ratio
        val h = w / ratio
        Box(
            Modifier
                .size(w, h)
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 14f * density
                }
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onFlip),
        ) {
            if (rotation <= 90f) {
                Box(frontModifier.fillMaxSize()) {
                    if (front != null) {
                        Image(
                            front, "Front of ${doc.title}",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().shadow(24.dp, CardShape).clip(CardShape),
                        )
                    } else {
                        IdCard(doc, Modifier.align(Alignment.Center))
                    }
                }
            } else {
                // Counter-rotate so the back isn't mirrored.
                Box(Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }, contentAlignment = Alignment.Center) {
                    if (back != null) {
                        Image(
                            back, "Back of ${doc.title}",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().shadow(24.dp, CardShape).clip(CardShape),
                        )
                    } else {
                        NoBackSide()
                    }
                }
            }
        }
    }
}

@Composable
private fun NoBackSide() {
    Column(
        Modifier
            .fillMaxSize()
            .clip(CardShape)
            .background(Palette.Surface2)
            .border(1.dp, Palette.Line2, CardShape)
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Rounded.FlipCameraAndroid, null, tint = Palette.Text3, modifier = Modifier.size(32.dp))
        Spacer(Modifier.height(10.dp))
        Text("No back photo yet", color = Palette.Text2, fontWeight = FontWeight.SemiBold)
        Text("Tap Edit to add the verso", color = Palette.Text3, fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}

/**
 * Draws a QR code from its module grid as vector shapes: razor-sharp at any size.
 * Data modules stay square (what scanners expect); only the three corner markers get soft
 * corners, which every scanner still reads.
 */
@Composable
fun QrCodeView(code: QrModules, modifier: Modifier = Modifier, ink: Color = QrInk) {
    Canvas(modifier.aspectRatio(1f)) {
        val n = code.size
        val quiet = 2
        // Whole pixels per module keeps every edge crisp.
        val cell = floor(size.minDimension / (n + quiet * 2)).coerceAtLeast(1f)
        val origin = Offset((size.width - cell * n) / 2, (size.height - cell * n) / 2)
        drawRect(Color.White)

        fun inFinder(x: Int, y: Int) = (x < 7 && y < 7) || (x >= n - 7 && y < 7) || (x < 7 && y >= n - 7)
        for (y in 0 until n) {
            var x = 0
            while (x < n) {
                if (!code[x, y] || inFinder(x, y)) { x++; continue }
                val start = x
                while (x < n && code[x, y] && !inFinder(x, y)) x++
                drawRect(ink, Offset(origin.x + start * cell, origin.y + y * cell), Size((x - start) * cell, cell))
            }
        }
        for ((fx, fy) in listOf(0 to 0, n - 7 to 0, 0 to n - 7)) {
            val o = Offset(origin.x + fx * cell, origin.y + fy * cell)
            drawRoundRect(ink, o, Size(7 * cell, 7 * cell), CornerRadius(cell * 1.8f))
            drawRoundRect(Color.White, o + Offset(cell, cell), Size(5 * cell, 5 * cell), CornerRadius(cell * 1.2f))
            drawRoundRect(ink, o + Offset(2 * cell, 2 * cell), Size(3 * cell, 3 * cell), CornerRadius(cell * 0.9f))
        }
    }
}

private val QrInk = Color(0xFF0B1022)

/** The QR code on a clean white card, at full screen brightness so scanners read it easily. */
@Composable
fun QrPanel(title: String, code: QrModules?, picture: ImageBitmap?, onClose: () -> Unit) {
    val activity = LocalContext.current as? Activity
    DisposableEffect(activity) {
        val window = activity?.window
        val previous = window?.attributes?.screenBrightness
        window?.let { w -> w.attributes = w.attributes.apply { screenBrightness = 1f } }
        onDispose {
            if (window != null && previous != null) window.attributes = window.attributes.apply { screenBrightness = previous }
        }
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xF2050710))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .padding(20.dp)
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .shadow(40.dp, RoundedCornerShape(32.dp), spotColor = Palette.Accent, ambientColor = Palette.Accent)
                .clip(RoundedCornerShape(32.dp))
                .background(Color.White)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .padding(horizontal = 22.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(Palette.AccentBrush),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.QrCode2, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(title, color = QrInk, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text("Ready to scan", color = Color(0xFF6B7387), fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(20.dp))
            when {
                code != null -> QrCodeView(code, Modifier.fillMaxWidth())
                picture != null -> Image(
                    picture, "QR code",
                    contentScale = ContentScale.Fit,
                    filterQuality = FilterQuality.None,
                    modifier = Modifier.fillMaxWidth().aspectRatio(picture.width.toFloat() / picture.height),
                )
                else -> Text("No QR code saved for this card", color = Color(0xFF6B7387), modifier = Modifier.padding(40.dp))
            }
            Spacer(Modifier.height(14.dp))
            if (code != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Verified, null, tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("High-definition code", color = Color(0xFF6B7387), fontSize = 13.sp)
                }
                Spacer(Modifier.height(18.dp))
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(QrInk)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Text("Done", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
        }
    }
}
