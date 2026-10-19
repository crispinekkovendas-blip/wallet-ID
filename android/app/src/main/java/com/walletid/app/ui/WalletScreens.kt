package com.walletid.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.walletid.app.data.Doc
import com.walletid.app.data.DocTypes
import com.walletid.app.data.ExpiryLevel

// ---------------------------------------------------------------- Lock

@Composable
fun LockScreen(canAuthenticate: Boolean, error: String?, onUnlock: () -> Unit, onOpenSecuritySettings: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Palette.Bg)) {
        Canvas(Modifier.fillMaxSize()) {
            val top = size.height * 0.24f
            drawRect(Brush.radialGradient(listOf(Palette.Accent.copy(alpha = 0.30f), Color.Transparent), Offset(size.width * 0.3f, top), size.width * 0.8f))
            drawRect(Brush.radialGradient(listOf(Palette.Accent2.copy(alpha = 0.24f), Color.Transparent), Offset(size.width * 0.78f, top * 0.8f), size.width * 0.75f))
        }
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                Modifier
                    .size(84.dp)
                    .shadow(30.dp, RoundedCornerShape(26.dp), spotColor = Palette.Accent, ambientColor = Palette.Accent)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF1C2A52), Color(0xFF2B1F55))))
                    .border(1.dp, Palette.Line2, RoundedCornerShape(26.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Shield, null, tint = Color(0xFFCFE0FF), modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.height(22.dp))
            Text("Wallet ID", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Palette.Text, letterSpacing = (-0.8).sp)
            Spacer(Modifier.height(10.dp))
            Text(
                if (canAuthenticate) "Your documents are locked.\nUse your fingerprint, face or phone PIN to open them."
                else "Set up a screen lock (PIN, pattern, fingerprint or face) on this phone to protect your documents.",
                color = Palette.Text2,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                lineHeight = 21.sp,
            )
            Spacer(Modifier.height(34.dp))
            if (canAuthenticate) {
                GradientButton("Unlock", onUnlock, Modifier.fillMaxWidth(), icon = Icons.Rounded.Fingerprint)
            } else {
                GradientButton("Open security settings", onOpenSecuritySettings, Modifier.fillMaxWidth(), icon = Icons.Rounded.Lock)
            }
            if (error != null) {
                Spacer(Modifier.height(14.dp))
                Text(error, color = Palette.Bad, fontSize = 14.sp, textAlign = TextAlign.Center)
            }
        }
        Row(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Lock, null, tint = Palette.Text3, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
            Text("AES-256 · Android Keystore · stored only on this phone", color = Palette.Text3, fontSize = 12.sp)
        }
    }
}

// ---------------------------------------------------------------- Wallet

@Composable
fun WalletScreen(
    docs: List<Doc>,
    thumbs: Map<String, ImageBitmap?>,
    totalCount: Int,
    query: String,
    onQuery: (String) -> Unit,
    onOpen: (String) -> Unit,
    onAdd: () -> Unit,
    onSettings: () -> Unit,
    onLock: () -> Unit,
    onImport: () -> Unit = {},
) {
    Box(Modifier.fillMaxSize().background(Palette.Bg)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(bottom = 120.dp),
        ) {
            Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 14.dp), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (totalCount == 0) "Wallet" else "$totalCount card${if (totalCount == 1) "" else "s"} · encrypted",
                        color = Palette.Text3,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Text("My wallet", style = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp, color = Palette.Text))
                }
                CircleIconButton(Icons.Rounded.Tune, "Settings", onSettings)
                Spacer(Modifier.width(8.dp))
                CircleIconButton(Icons.Rounded.Lock, "Lock", onLock)
            }

            if (totalCount == 0) {
                EmptyWallet(onAdd, onImport)
                return@Column
            }

            SearchField(query, onQuery, Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(14.dp))

            if (query.isBlank()) {
                val attention = docs.mapNotNull { d -> d.expiry?.let { d to it } }.sortedBy { it.second.days }.take(3)
                attention.forEach { (d, e) ->
                    val bad = e.level == ExpiryLevel.BAD
                    val tint = if (bad) Color(0xFFFECACA) else Color(0xFFFDE68A)
                    Row(
                        Modifier
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background((if (bad) Palette.Bad else Palette.Warn).copy(alpha = 0.09f))
                            .border(1.dp, (if (bad) Palette.Bad else Palette.Warn).copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                            .clickable { onOpen(d.id) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.WarningAmber, null, tint = tint, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(d.title, color = tint, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(" · ${e.text}", color = tint, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Icon(Icons.Rounded.ChevronRight, null, tint = tint.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                    }
                }
                if (attention.isNotEmpty()) Spacer(Modifier.height(12.dp))
            }

            if (docs.isEmpty()) {
                Text(
                    "No cards match “${query.trim()}”",
                    color = Palette.Text3,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(40.dp),
                )
            }

            val stacked = query.isBlank() && docs.size > 1
            Column(
                Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(if (stacked) 0.dp else 16.dp),
            ) {
                docs.forEachIndexed { i, d ->
                    val last = i == docs.lastIndex
                    PhotoCard(
                        d,
                        thumbs[d.id],
                        Modifier
                            .then(if (stacked && !last) Modifier.peek(84.dp) else Modifier)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onOpen(d.id) },
                    )
                }
            }
        }

        if (totalCount > 0) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 20.dp, bottom = 24.dp)
                    .size(64.dp)
                    .shadow(22.dp, RoundedCornerShape(22.dp), spotColor = Palette.Accent, ambientColor = Palette.Accent)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Palette.AccentBrush)
                    .clickable(onClick = onAdd),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Add, "Add card", tint = Color.White, modifier = Modifier.size(30.dp))
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQuery: (String) -> Unit, modifier: Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Palette.Line, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Search, null, tint = Palette.Text3, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) Text("Search cards…", color = Palette.Text3, fontSize = 16.sp)
            BasicTextField(
                query,
                onQuery,
                singleLine = true,
                textStyle = TextStyle(color = Palette.Text, fontSize = 16.sp),
                cursorBrush = SolidColor(Palette.Accent),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun EmptyWallet(onAdd: () -> Unit, onImport: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.width(230.dp).height(170.dp)) {
            val cards = listOf(
                Triple(DocTypes.get("rg"), -8f, (-16).dp),
                Triple(DocTypes.get("cnh"), 4f, 12.dp),
                Triple(DocTypes.get("creci"), 0f, 0.dp),
            )
            cards.forEachIndexed { i, (t, angle, dx) ->
                TypeSwatch(
                    t.c1, t.c2, t.ink, t.gold, if (i == 2) t.abbr else "",
                    Modifier
                        .padding(horizontal = 10.dp)
                        .offset(x = dx, y = (i * 12).dp)
                        .rotate(angle)
                        .shadow(16.dp, RoundedCornerShape(10.dp)),
                )
            }
        }
        Spacer(Modifier.height(26.dp))
        Text("Your wallet is empty", color = Palette.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Add a photo of your RG, CNH, CRECI or any other card. Each one is encrypted before it's saved.",
            color = Palette.Text2,
            textAlign = TextAlign.Center,
            fontSize = 15.sp,
            lineHeight = 21.sp,
        )
        Spacer(Modifier.height(24.dp))
        GradientButton("Add card", onAdd, icon = Icons.Rounded.Add)
        Spacer(Modifier.height(12.dp))
        TextAction("Import a backup", onImport)
    }
}
