package com.walletid.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.walletid.app.EditSession
import com.walletid.app.data.Doc
import com.walletid.app.data.DocTypes
import com.walletid.app.data.ExpiryLevel
import com.walletid.app.data.FieldDef
import com.walletid.app.data.FieldKind
import com.walletid.app.data.Side
import com.walletid.app.data.formatBrDate
import com.walletid.app.data.formatCpf
import com.walletid.app.data.mask
import com.walletid.app.data.parseIso
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

// ---------------------------------------------------------------- Detail

@Composable
fun DetailScreen(
    doc: Doc,
    reveal: Boolean,
    front: ImageBitmap?,
    back: ImageBitmap?,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onToggleReveal: () -> Unit,
    onCopy: (label: String, value: String) -> Unit,
    onShare: () -> Unit,
    onOpenPhoto: (Side) -> Unit,
    onDelete: () -> Unit,
) {
    val t = doc.type
    val hasSecret = t.fields.any { it.secret && doc[it.key].isNotEmpty() }
    val primary = doc.primaryField
    var confirmDelete by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(Palette.Bg)) {
        NavBar(
            doc.title,
            left = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBackIos, "Back", tint = Palette.Text, modifier = Modifier.size(20.dp)) } },
            right = { TextAction("Edit", onEdit, strong = true) },
        )
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp),
        ) {
            Spacer(Modifier.height(22.dp))
            IdCard(doc, reveal = reveal)
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ActionButton(Icons.Rounded.ContentCopy, "Copy no.", enabled = doc.primaryValue.isNotEmpty()) {
                    primary?.let { onCopy(it.label.substringBefore(" ("), doc.primaryValue) }
                }
                if (hasSecret) {
                    ActionButton(if (reveal) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, if (reveal) "Hide" else "Reveal", onClick = onToggleReveal)
                }
                ActionButton(Icons.Rounded.Share, "Share", enabled = front != null || back != null, onClick = onShare)
                ActionButton(Icons.Rounded.Image, "Photo", enabled = front != null || back != null) {
                    onOpenPhoto(if (front != null) Side.FRONT else Side.BACK)
                }
            }

            if (front != null || back != null) {
                SectionTitle("Photos")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf(Side.FRONT to front, Side.BACK to back).forEach { (side, img) ->
                        Box(Modifier.weight(1f)) {
                            if (img != null) PhotoThumb(img, if (side == Side.FRONT) "Front" else "Back") { onOpenPhoto(side) }
                        }
                    }
                }
            }

            SectionTitle("Details")
            Group {
                val rows = t.fields.filter { doc[it.key].isNotEmpty() }
                if (rows.isEmpty() && doc["notes"].isEmpty()) {
                    Text("No details yet. Tap Edit to add some.", color = Palette.Text3, modifier = Modifier.padding(16.dp))
                }
                rows.forEachIndexed { i, f ->
                    if (i > 0) GroupDivider()
                    DetailRow(f, doc[f.key], reveal, doc.expiry?.takeIf { f.key == "expires" }?.let { it.text to (it.level == ExpiryLevel.BAD) }, onCopy)
                }
                if (doc["notes"].isNotEmpty()) {
                    if (rows.isNotEmpty()) GroupDivider()
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text("Notes", color = Palette.Text3, fontSize = 12.sp)
                        Text(doc["notes"], color = Palette.Text2, fontSize = 16.sp)
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
            Group { RowButton(Icons.Rounded.DeleteOutline, "Delete document", { confirmDelete = true }, danger = true) }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete ${doc.title}?",
            text = "This document and its photos will be permanently removed from this phone.",
            confirm = "Delete",
            danger = true,
            onConfirm = { confirmDelete = false; onDelete() },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun DetailRow(f: FieldDef, raw: String, reveal: Boolean, expiry: Pair<String, Boolean>?, onCopy: (String, String) -> Unit) {
    val shown = when {
        f.kind == FieldKind.DATE -> formatBrDate(raw)
        f.secret && !reveal -> mask(raw)
        else -> raw
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(f.label, color = Palette.Text3, fontSize = 12.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    shown,
                    color = Palette.Text,
                    fontSize = 16.sp,
                    fontFamily = if (f.mono) FontFamily.Monospace else FontFamily.Default,
                )
                if (expiry != null) {
                    Spacer(Modifier.width(8.dp))
                    Pill(expiry.first, if (expiry.second) Palette.Bad else Palette.Warn)
                }
            }
        }
        if (f.mono || f.kind == FieldKind.CPF) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .clickable { onCopy(f.label.substringBefore(" ("), raw) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.ContentCopy, "Copy ${f.label}", tint = Palette.Text2, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun ActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Column(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(4.dp)
            .widthIn(min = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Palette.Surface2)
                .border(1.dp, Palette.Line, RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = if (enabled) Palette.Text else Palette.Text3.copy(alpha = 0.5f), modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(label, color = if (enabled) Palette.Text2 else Palette.Text3.copy(alpha = 0.5f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun PhotoThumb(img: ImageBitmap, label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1.586f)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, Palette.Line, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
    ) {
        Image(img, label, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Text(
            label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

// ---------------------------------------------------------------- Edit

@Composable
fun EditScreen(
    session: EditSession,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    onChoosePhoto: (Side) -> Unit,
    onTakePhoto: (Side) -> Unit,
    onRemovePhoto: (Side) -> Unit,
) {
    val type = DocTypes.get(session.typeKey)
    val title = when {
        session.id != null -> "Edit ${session.asDoc().title}"
        type.key == "other" -> "New document"
        else -> "New ${type.abbr}"
    }
    Column(Modifier.fillMaxSize().background(Palette.Bg).imePadding()) {
        NavBar(title, left = { TextAction("Cancel", onCancel) }, right = { TextAction("Save", onSave, strong = true) })
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp),
        ) {
            Spacer(Modifier.height(18.dp))
            IdCard(session.asDoc(), reveal = true, modifier = Modifier.padding(horizontal = 18.dp))

            SectionTitle("Photos")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (side in Side.entries) {
                    PhotoSlot(
                        image = session.previews[side],
                        title = if (side == Side.FRONT) "Front" else "Back",
                        hint = if (side == Side.FRONT) "Screenshot or photo" else "Optional",
                        onChoose = { onChoosePhoto(side) },
                        onCamera = { onTakePhoto(side) },
                        onRemove = { onRemovePhoto(side) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            SectionTitle("Details")
            Group {
                type.fields.forEachIndexed { i, f ->
                    if (i > 0) GroupDivider()
                    FieldEditor(f, session.fields[f.key].orEmpty()) { session.fields[f.key] = it }
                }
            }

            SectionTitle("Notes")
            Group {
                FieldEditor(FieldDef("notes", "Notes", FieldKind.MULTILINE, placeholder = "Anything else worth remembering"), session.fields["notes"].orEmpty()) {
                    session.fields["notes"] = it
                }
            }
        }
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
    modifier: Modifier,
) {
    var menu by remember { mutableStateOf(false) }
    Box(modifier) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1.586f)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White.copy(alpha = 0.02f))
                .clickable { menu = true },
            contentAlignment = Alignment.Center,
        ) {
            if (image != null) {
                Image(image, title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable(onClick = onRemove),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Close, "Remove $title", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            } else {
                Canvas(Modifier.fillMaxSize()) {
                    drawRoundRect(
                        Palette.Line2,
                        cornerRadius = CornerRadius(18.dp.toPx()),
                        style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))),
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.PhotoCamera, null, tint = Palette.Accent, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.height(4.dp))
                    Text(title, color = Palette.Text2, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(hint, color = Palette.Text3, fontSize = 12.sp)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FieldEditor(f: FieldDef, value: String, onChange: (String) -> Unit) {
    var datePicker by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    val clickable = f.kind == FieldKind.DATE || f.kind == FieldKind.SELECT

    Box {
        Column(
            Modifier
                .fillMaxWidth()
                .then(
                    if (clickable) Modifier.clickable {
                        if (f.kind == FieldKind.DATE) {
                            datePicker = true
                        } else {
                            menu = true
                        }
                    } else Modifier,
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(f.label + if (f.required) " *" else "", color = Palette.Text3, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(3.dp))
            when (f.kind) {
                FieldKind.DATE, FieldKind.SELECT -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        when {
                            value.isEmpty() -> if (f.kind == FieldKind.DATE) "Select date" else "Select…"
                            f.kind == FieldKind.DATE -> formatBrDate(value)
                            else -> value
                        },
                        color = if (value.isEmpty()) Palette.Text3.copy(alpha = 0.7f) else Palette.Text,
                        fontSize = 17.sp,
                        modifier = Modifier.weight(1f),
                    )
                    if (value.isNotEmpty() && f.kind == FieldKind.DATE) {
                        Icon(
                            Icons.Rounded.Close, "Clear", tint = Palette.Text3,
                            modifier = Modifier.size(20.dp).clip(CircleShape).clickable { onChange("") },
                        )
                        Spacer(Modifier.width(10.dp))
                    }
                    Icon(
                        if (f.kind == FieldKind.DATE) Icons.Rounded.CalendarMonth else Icons.Rounded.ExpandMore,
                        null, tint = Palette.Text3, modifier = Modifier.size(20.dp),
                    )
                }
                else -> Box {
                    if (value.isEmpty() && f.placeholder.isNotEmpty()) {
                        Text(f.placeholder, color = Palette.Text3.copy(alpha = 0.6f), fontSize = 17.sp)
                    } else if (value.isEmpty() && f.kind == FieldKind.CPF) {
                        Text("000.000.000-00", color = Palette.Text3.copy(alpha = 0.6f), fontSize = 17.sp, fontFamily = FontFamily.Monospace)
                    }
                    BasicTextField(
                        value,
                        { onChange(if (f.kind == FieldKind.CPF) formatCpf(it) else it) },
                        singleLine = f.kind != FieldKind.MULTILINE,
                        minLines = if (f.kind == FieldKind.MULTILINE) 2 else 1,
                        textStyle = TextStyle(
                            color = Palette.Text,
                            fontSize = 17.sp,
                            fontFamily = if (f.mono || f.kind == FieldKind.CPF) FontFamily.Monospace else FontFamily.Default,
                        ),
                        cursorBrush = SolidColor(Palette.Accent),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (f.kind == FieldKind.CPF) KeyboardType.Number else KeyboardType.Text,
                            capitalization = if (f.key == "holder") KeyboardCapitalization.Words else KeyboardCapitalization.Sentences,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        if (f.kind == FieldKind.SELECT) {
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = Palette.Surface2) {
                f.options.forEach { opt ->
                    DropdownMenuItem(text = { Text(opt.ifEmpty { "None" }) }, onClick = { menu = false; onChange(opt) })
                }
            }
        }
    }

    if (datePicker) {
        val initial = parseIso(value) ?: LocalDate.now()
        val state = rememberDatePickerState(initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { datePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString()) }
                    datePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { datePicker = false }) { Text("Cancel") } },
        ) {
            DatePicker(state)
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

// ---------------------------------------------------------------- Photo viewer

@Composable
fun PhotoViewer(image: ImageBitmap, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 6f)
                        offset = if (scale == 1f) Offset.Zero else offset + pan
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                        }
                    })
                },
        ) {
            Image(
                image,
                null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y),
            )
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(14.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.14f))
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Close, "Close", tint = Color.White)
            }
        }
    }
}
