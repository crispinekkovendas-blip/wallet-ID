package com.walletid.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.walletid.app.EditSession
import com.walletid.app.data.Doc
import com.walletid.app.data.QrModules
import com.walletid.app.data.Side

// ---------------------------------------------------------------- Card (full screen)

@Composable
fun CardScreen(
    doc: Doc,
    front: ImageBitmap?,
    back: ImageBitmap?,
    hasQr: Boolean,
    qr: QrModules?,
    qrPicture: ImageBitmap?,
    showQr: Boolean,
    flipped: Boolean,
    onFlip: () -> Unit,
    onShowQr: (Boolean) -> Unit,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onShare: (Side) -> Unit,
    onDelete: () -> Unit,
    animateIn: Boolean = true,
) {
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.radialGradient(listOf(Color(0xFF141B30), Color.Black), radius = 1600f)),
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Rounded.Close, "Close", tint = Color.White) }
                Text(
                    doc.title,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "More", tint = Color.White) }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = Palette.Surface2) {
                        DropdownMenuItem(text = { Text("Edit") }, leadingIcon = { Icon(Icons.Rounded.Edit, null) }, onClick = { menu = false; onEdit() })
                        if (front != null) {
                            DropdownMenuItem(text = { Text("Share front") }, leadingIcon = { Icon(Icons.Rounded.Share, null) }, onClick = { menu = false; onShare(Side.FRONT) })
                        }
                        if (back != null) {
                            DropdownMenuItem(text = { Text("Share back") }, leadingIcon = { Icon(Icons.Rounded.Share, null) }, onClick = { menu = false; onShare(Side.BACK) })
                        }
                        DropdownMenuItem(
                            text = { Text("Delete", color = Palette.Bad) },
                            leadingIcon = { Icon(Icons.Rounded.DeleteOutline, null, tint = Palette.Bad) },
                            onClick = { menu = false; confirmDelete = true },
                        )
                    }
                }
            }

            PortraitCard(
                doc, front, back, flipped, onFlip,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                animateIn = animateIn,
            )

            Column(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (hasQr) {
                    CertificateBadge(onClick = { onShowQr(true) })
                } else {
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(50))
                            .clickable(onClick = onEdit)
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.QrCode2, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Add QR code", color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.TouchApp, null, tint = Color.White.copy(alpha = 0.45f), modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (flipped) "Tap the card to see the front" else "Tap the card to see the back",
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 13.sp,
                    )
                }
            }
        }
        if (showQr) QrPanel(doc.title, qr, qrPicture) { onShowQr(false) }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete ${doc.title}?",
            text = "This card and its photos will be permanently removed from this phone.",
            confirm = "Delete",
            danger = true,
            onConfirm = { confirmDelete = false; onDelete() },
            onDismiss = { confirmDelete = false },
        )
    }
}

// ---------------------------------------------------------------- Add / edit

/** Quick labels shown as chips: (type key, name). */
private val QUICK_LABELS = listOf(
    "rg" to "RG", "cnh" to "CNH", "cpf" to "CPF", "creci" to "CRECI", "cin" to "CIN", "passport" to "Passaporte",
    "titulo" to "Título de Eleitor", "sus" to "Cartão SUS", "health" to "Plano de saúde", "council" to "OAB",
    "ctps" to "CTPS", "crlv" to "CRLV", "other" to "Outro",
)

@Composable
fun EditScreen(
    session: EditSession,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    onChoosePhoto: (Side) -> Unit,
    onTakePhoto: (Side) -> Unit,
    onRemovePhoto: (Side) -> Unit,
    onRotatePhoto: (Side) -> Unit,
    onLabel: (typeKey: String, title: String) -> Unit,
    onQrFromImage: () -> Unit,
    onQrText: (String) -> Unit,
) {
    var typing by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(Palette.Bg).imePadding()) {
        NavBar(
            if (session.id == null) "New card" else "Edit card",
            left = { TextAction("Cancel", onCancel) },
            right = { TextAction("Save", onSave, strong = true, enabled = session.previews[Side.FRONT] != null) },
        )
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp),
        ) {
            SectionTitle("Front")
            PhotoSlot(session.previews[Side.FRONT], "Add the front of the card", "Screenshot or photo",
                { onChoosePhoto(Side.FRONT) }, { onTakePhoto(Side.FRONT) }, { onRemovePhoto(Side.FRONT) }, { onRotatePhoto(Side.FRONT) })

            SectionTitle("Back (verso)")
            PhotoSlot(session.previews[Side.BACK], "Add the back", "Optional · shown when you flip the card",
                { onChoosePhoto(Side.BACK) }, { onTakePhoto(Side.BACK) }, { onRemovePhoto(Side.BACK) }, { onRotatePhoto(Side.BACK) })

            SectionTitle("Name")
            Group {
                Box(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    if (session.title.isEmpty()) Text("e.g. CNH", color = Palette.Text3.copy(alpha = 0.6f), fontSize = 17.sp)
                    BasicTextField(
                        session.title,
                        { session.fields["title"] = it },
                        singleLine = true,
                        textStyle = TextStyle(color = Palette.Text, fontSize = 17.sp),
                        cursorBrush = SolidColor(Palette.Accent),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                QUICK_LABELS.forEach { (key, name) ->
                    val selected = session.title == name
                    Text(
                        name,
                        color = if (selected) Color.White else Palette.Text2,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (selected) Palette.Accent else Palette.Surface)
                            .border(1.dp, if (selected) Palette.Accent else Palette.Line2, RoundedCornerShape(50))
                            .clickable { onLabel(key, name) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }

            SectionTitle("QR code")
            Group {
                val code = session.qrCode
                val picture = session.previews[Side.QR]
                when {
                    session.scanning -> Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(20.dp), color = Palette.Accent, strokeWidth = 2.dp)
                        Spacer(Modifier.width(12.dp))
                        Text("Looking for a QR code…", color = Palette.Text2)
                    }
                    code != null || picture != null -> Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        val thumb = Modifier.size(72.dp).clip(RoundedCornerShape(10.dp)).background(Color.White).padding(4.dp)
                        if (code != null) {
                            QrCodeView(code, thumb)
                        } else if (picture != null) {
                            Image(picture, "QR code", filterQuality = FilterQuality.None, modifier = thumb)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("QR code saved", color = Palette.Text, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (code != null) "Exact code · shown in high definition" else "Saved as a picture",
                                color = Palette.Text3,
                                fontSize = 13.sp,
                            )
                        }
                        IconButton(onClick = { onRemovePhoto(Side.QR) }) { Icon(Icons.Rounded.Close, "Remove QR code", tint = Palette.Text3) }
                    }
                    else -> Column {
                        Text(
                            "We look for a QR code in your photos automatically. You can also add one from a screenshot or type it.",
                            color = Palette.Text3,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 4.dp),
                        )
                        RowButton(Icons.Rounded.QrCodeScanner, "Add QR from image", onQrFromImage)
                        GroupDivider()
                        RowButton(Icons.Rounded.Keyboard, "Type code or link", { typing = true })
                    }
                }
            }
        }
    }

    if (typing) {
        var text by remember { mutableStateOf(session.fields["qr"].orEmpty()) }
        AlertDialog(
            onDismissRequest = { typing = false },
            containerColor = Palette.Surface,
            title = { Text("QR code content", color = Palette.Text) },
            text = {
                OutlinedTextField(
                    text,
                    { text = it },
                    placeholder = { Text("Text or link") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Palette.Accent, unfocusedBorderColor = Palette.Line2),
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = { typing = false; onQrText(text) }, enabled = text.isNotBlank()) { Text("Save", fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = { TextButton(onClick = { typing = false }) { Text("Cancel", color = Palette.Text2) } },
        )
    }
}

@Composable
private fun PhotoSlot(
    image: ImageBitmap?,
    title: String,
    hint: String,
    onChoose: () -> Unit,
    onCamera: () -> Unit,
    onRemove: () -> Unit,
    onRotate: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Box {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(CARD_RATIO)
                .clip(RoundedCornerShape(18.dp))
                .background(if (image != null) Palette.Surface2 else Color.White.copy(alpha = 0.02f))
                .clickable { menu = true },
            contentAlignment = Alignment.Center,
        ) {
            if (image != null) {
                Image(image, title, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                Row(Modifier.align(Alignment.TopEnd).padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PhotoAction(Icons.AutoMirrored.Rounded.RotateRight, "Rotate photo", onRotate)
                    PhotoAction(Icons.Rounded.Close, "Remove photo", onRemove)
                }
            } else {
                Canvas(Modifier.fillMaxSize()) {
                    drawRoundRect(
                        Palette.Line2,
                        cornerRadius = CornerRadius(18.dp.toPx()),
                        style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f))),
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(52.dp).clip(CircleShape).background(Palette.Accent.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.PhotoCamera, null, tint = Palette.Accent, modifier = Modifier.size(26.dp))
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(title, color = Palette.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(hint, color = Palette.Text3, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = Palette.Surface2) {
            DropdownMenuItem(
                text = { Text("Choose photo or screenshot") },
                leadingIcon = { Icon(Icons.Rounded.PhotoLibrary, null) },
                onClick = { menu = false; onChoose() },
            )
            DropdownMenuItem(
                text = { Text("Take photo") },
                leadingIcon = { Icon(Icons.Rounded.PhotoCamera, null) },
                onClick = { menu = false; onCamera() },
            )
        }
    }
}

// ---------------------------------------------------------------- Settings

@Composable
fun SettingsScreen(
    lockAfterSeconds: Int,
    version: String,
    onLockAfter: (Int) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onErase: () -> Unit,
    onBack: () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(Palette.Bg)) {
        NavBar("Settings", left = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBackIos, "Back", tint = Palette.Text, modifier = Modifier.size(20.dp)) }
        })
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp),
        ) {
            Spacer(Modifier.height(22.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Palette.Accent.copy(alpha = 0.14f), Palette.Accent2.copy(alpha = 0.10f))))
                    .border(1.dp, Palette.Accent.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
                    .padding(18.dp),
            ) {
                Icon(Icons.Rounded.Shield, null, tint = Color(0xFFA9C2FF), modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("Private by design", color = Palette.Text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Documents are encrypted with AES-256-GCM using a key locked inside this phone's Android Keystore. " +
                            "Nothing is uploaded, screenshots of the app are blocked, and the app has no internet permission.",
                        color = Palette.Text2,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                    )
                }
            }

            SectionTitle("Auto-lock")
            Group {
                listOf(0 to "Immediately when I leave the app", 60 to "After 1 minute", 300 to "After 5 minutes").forEachIndexed { i, (secs, label) ->
                    if (i > 0) GroupDivider()
                    Row(
                        Modifier.fillMaxWidth().clickable { onLockAfter(secs) }.padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Schedule, null, tint = Palette.Accent, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(14.dp))
                        Text(label, color = Palette.Text, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        RadioButton(
                            selected = lockAfterSeconds == secs,
                            onClick = { onLockAfter(secs) },
                            colors = RadioButtonDefaults.colors(selectedColor = Palette.Accent, unselectedColor = Palette.Text3),
                        )
                    }
                }
            }

            SectionTitle("Backup")
            Group {
                RowButton(Icons.Rounded.Download, "Export encrypted backup", onExport)
                GroupDivider()
                RowButton(Icons.Rounded.Upload, "Import backup", onImport)
            }
            Text(
                "Backups are protected with a passphrase you choose and work with the Wallet ID web app too. " +
                    "Keep one somewhere safe: if you lose this phone, it's the only way to get your documents back.",
                color = Palette.Text3,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(start = 6.dp, top = 8.dp, end = 6.dp),
            )

            Spacer(Modifier.height(28.dp))
            Group { RowButton(Icons.Rounded.DeleteOutline, "Erase all data", onErase, danger = true) }

            Text("Wallet ID $version", color = Palette.Text3, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 30.dp))
        }
    }
}

// ---------------------------------------------------------------- Dialogs

@Composable
fun ConfirmDialog(title: String, text: String, confirm: String, danger: Boolean = false, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.Surface,
        title = { Text(title, color = Palette.Text) },
        text = { Text(text, color = Palette.Text2) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirm, color = if (danger) Palette.Bad else Palette.Accent, fontWeight = FontWeight.SemiBold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Palette.Text2) } },
    )
}

/** Asks for a passphrase. With [repeat], asks twice and enforces 8+ characters (for new backups). */
@Composable
fun PassphraseDialog(title: String, text: String, confirm: String, repeat: Boolean, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var p1 by remember { mutableStateOf("") }
    var p2 by remember { mutableStateOf("") }
    val error = when {
        !repeat -> null
        p1.isNotEmpty() && p1.length < 8 -> "Use at least 8 characters"
        p2.isNotEmpty() && p1 != p2 -> "Passphrases don’t match"
        else -> null
    }
    val ok = if (repeat) p1.length >= 8 && p1 == p2 else p1.isNotEmpty()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.Surface,
        icon = { Icon(Icons.Rounded.Key, null, tint = Palette.Accent) },
        title = { Text(title, color = Palette.Text) },
        text = {
            Column {
                Text(text, color = Palette.Text2, fontSize = 14.sp)
                Spacer(Modifier.height(14.dp))
                PassField(p1, { p1 = it }, "Passphrase")
                if (repeat) {
                    Spacer(Modifier.height(8.dp))
                    PassField(p2, { p2 = it }, "Repeat passphrase")
                }
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(error, color = Palette.Bad, fontSize = 13.sp)
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(p1) }, enabled = ok) { Text(confirm, fontWeight = FontWeight.SemiBold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Palette.Text2) } },
    )
}

@Composable
private fun PassField(value: String, onChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value,
        onChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Palette.Accent, unfocusedBorderColor = Palette.Line2),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun EraseDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    var typed by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.Surface,
        title = { Text("Erase all data?", color = Palette.Text) },
        text = {
            Column {
                Text(
                    "Every document and photo will be deleted from this phone. Export a backup first if you want to keep them. Type ERASE to confirm.",
                    color = Palette.Text2,
                    fontSize = 14.sp,
                )
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    typed,
                    { typed = it },
                    singleLine = true,
                    placeholder = { Text("ERASE") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Palette.Bad, unfocusedBorderColor = Palette.Line2),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = typed.trim().equals("ERASE", ignoreCase = true)) {
                Text("Erase everything", color = Palette.Bad, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Palette.Text2) } },
    )
}


@Composable
private fun PhotoAction(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.62f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, description, tint = Color.White, modifier = Modifier.size(19.dp))
    }
}
