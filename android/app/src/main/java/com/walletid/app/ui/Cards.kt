package com.walletid.app.ui

import android.app.Activity
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
        Image(image, doc.title, contentScale = ContentScale.Crop, alignment = Alignment.TopCenter, modifier = Modifier.fillMaxSize())
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

/** The QR code on a white panel, at full screen brightness so scanners read it easily. */
@Composable
fun QrPanel(title: String, qr: ImageBitmap?, onClose: () -> Unit) {
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
            .background(Color.Black.copy(alpha = 0.88f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .padding(24.dp)
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, color = Color(0xFF0B1022), fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Show this code to be scanned", color = Color(0xFF5B6478), fontSize = 14.sp)
            Spacer(Modifier.height(20.dp))
            if (qr != null) {
                Image(
                    qr, "QR code",
                    contentScale = ContentScale.Fit,
                    // Keep modules sharp when scaling the code up.
                    filterQuality = FilterQuality.None,
                    modifier = Modifier.fillMaxWidth().aspectRatio(qr.width.toFloat() / qr.height),
                )
            } else {
                Text("No QR code saved for this card", color = Color(0xFF5B6478), modifier = Modifier.padding(40.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "Close",
                color = Color(0xFF3D63E0),
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onClose)
                    .padding(horizontal = 24.dp, vertical = 10.dp),
            )
        }
    }
}
