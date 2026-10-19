package com.prenatal.app.ui

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prenatal.app.data.Attachment
import com.prenatal.app.data.Gestation
import com.prenatal.app.data.Result
import com.prenatal.app.data.ResultCategory

typealias Thumbs = suspend (Attachment) -> ImageBitmap?

private fun categoryColor(c: ResultCategory) = when (c) {
    ResultCategory.LAB -> P.Sky
    ResultCategory.ULTRASOUND -> P.Lavender
    ResultCategory.OTHER -> P.Amber
}

@Composable
fun ResultsScreen(results: List<Result>, due: java.time.LocalDate?, thumbs: Thumbs, onOpen: (String) -> Unit, onAdd: () -> Unit) {
    var filter by remember { mutableStateOf<ResultCategory?>(null) }
    val shown = results.filter { filter == null || it.category == filter }.sortedByDescending { it.date }
    Column(Modifier.fillMaxSize().background(P.Bg).verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Results", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
            PrimaryButton("Add", onAdd, icon = Icons.Rounded.Add)
        }
        Spacer(Modifier.height(16.dp))
        Segmented(listOf("All", "Lab", "Ultrasound", "Other"), when (filter) {
            null -> 0
            ResultCategory.LAB -> 1
            ResultCategory.ULTRASOUND -> 2
            ResultCategory.OTHER -> 3
        }) { filter = listOf(null, ResultCategory.LAB, ResultCategory.ULTRASOUND, ResultCategory.OTHER)[it] }
        Spacer(Modifier.height(14.dp))
        if (shown.isEmpty()) {
            EmptyState("🔬", "No results yet", "Keep lab results, ultrasound reports and photos of exams here, with your notes.")
        }
        shown.forEach { r ->
            val color = categoryColor(r.category)
            SoftCard(onClick = { onOpen(r.id) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val first = r.attachments.firstOrNull()
                    if (first != null) {
                        val img by produceState<ImageBitmap?>(null, first.id) { value = thumbs(first) }
                        Box(Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).background(color.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                            img?.let { Image(it, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                                ?: Icon(Icons.Rounded.Description, null, tint = color)
                        }
                    } else {
                        IconBubble(Icons.Rounded.Science, color, 56)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(r.title.ifEmpty { "Result" }, color = P.Text, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        val wk = due?.let { " · ${Gestation.weeksAt(it, r.date)}w" }.orEmpty()
                        Text(r.date.format(BR_DATE) + wk, color = P.Text2, fontSize = 13.sp)
                        if (r.values.isNotBlank()) {
                            Text(r.values.lines().first(), color = P.Text3, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Chip(r.category.label, color)
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(120.dp))
    }
}

@Composable
fun ResultViewScreen(
    r: Result,
    due: java.time.LocalDate?,
    thumbs: Thumbs,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onOpenAttachment: (Attachment) -> Unit,
    onDelete: () -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(P.Bg)) {
        TopBar(
            r.category.label,
            left = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBackIos, "Back", tint = P.Text, modifier = Modifier.size(20.dp)) } },
            right = { TextAction("Edit", onEdit, strong = true) },
        )
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(18.dp))
            Text(r.title.ifEmpty { "Result" }, color = P.Text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            val wk = due?.let { " · ${Gestation.weeksAt(it, r.date)} weeks" }.orEmpty()
            Text(r.date.format(BR_DATE) + wk, color = P.Text2)
            if (r.values.isNotBlank()) {
                SectionHeader("Values")
                SoftCard {
                    r.values.lines().filter { it.isNotBlank() }.forEach { line ->
                        val parts = line.split(":", limit = 2)
                        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                            if (parts.size == 2) {
                                Text(parts[0].trim(), color = P.Text2, modifier = Modifier.weight(1f))
                                Text(parts[1].trim(), color = P.Text, fontWeight = FontWeight.SemiBold)
                            } else {
                                Text(line.trim(), color = P.Text)
                            }
                        }
                    }
                }
            }
            if (r.notes.isNotBlank()) {
                SectionHeader("Notes")
                SoftCard { Text(r.notes, color = P.Text) }
            }
            if (r.attachments.isNotEmpty()) {
                SectionHeader("Files")
                r.attachments.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { a -> AttachmentTile(a, thumbs, Modifier.weight(1f)) { onOpenAttachment(a) } }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).border(1.dp, P.Line, RoundedCornerShape(16.dp))
                    .clickable { confirmDelete = true }.padding(14.dp),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.DeleteOutline, null, tint = P.Bad)
                Spacer(Modifier.width(8.dp))
                Text("Delete result", color = P.Bad, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(40.dp))
        }
    }
    if (confirmDelete) {
        ConfirmDialog("Delete this result?", "The result and its files will be removed from this phone.", "Delete", danger = true,
            onConfirm = { confirmDelete = false; onDelete() }, onDismiss = { confirmDelete = false })
    }
}

@Composable
private fun AttachmentTile(a: Attachment, thumbs: Thumbs, modifier: Modifier, onRemove: (() -> Unit)? = null, onClick: () -> Unit) {
    val img by produceState<ImageBitmap?>(null, a.id) { value = thumbs(a) }
    Column(modifier) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(0.8f).clip(RoundedCornerShape(16.dp)).background(P.Surface2)
                .border(1.dp, P.Line, RoundedCornerShape(16.dp)).clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            img?.let { Image(it, a.name, contentScale = ContentScale.Crop, alignment = Alignment.TopCenter, modifier = Modifier.fillMaxSize()) }
                ?: CircularProgressIndicator(color = P.Rose, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
            if (a.mime == "application/pdf") {
                Icon(Icons.Rounded.PictureAsPdf, "PDF", tint = P.Bad, modifier = Modifier.align(Alignment.BottomStart).padding(8.dp).size(22.dp))
            }
            if (onRemove != null) {
                Box(
                    Modifier.align(Alignment.TopEnd).padding(8.dp).size(30.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.55f)).clickable(onClick = onRemove),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Close, "Remove", tint = Color.White, modifier = Modifier.size(16.dp)) }
            }
        }
        Text(a.name, color = P.Text2, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp, start = 2.dp))
    }
}

@Composable
fun ResultEditScreen(
    initial: Result,
    isNew: Boolean,
    thumbs: Thumbs,
    newAttachments: List<Attachment>,
    onPickPhoto: () -> Unit,
    onTakePhoto: () -> Unit,
    onPickFile: () -> Unit,
    onCancel: () -> Unit,
    onSave: (Result, removed: List<Attachment>) -> Unit,
) {
    var title by remember { mutableStateOf(initial.title) }
    var date by remember { mutableStateOf(initial.date) }
    var category by remember { mutableStateOf(initial.category) }
    var values by remember { mutableStateOf(initial.values) }
    var notes by remember { mutableStateOf(initial.notes) }
    val kept = remember { mutableStateListOf<Attachment>().apply { addAll(initial.attachments) } }
    val removed = remember { mutableStateListOf<Attachment>() }
    var menu by remember { mutableStateOf(false) }
    val all = kept + newAttachments.filter { n -> removed.none { it.id == n.id } }

    Column(Modifier.fillMaxSize().background(P.Bg).imePadding()) {
        TopBar(
            if (isNew) "New result" else "Edit result",
            left = { TextAction("Cancel", onCancel) },
            right = {
                TextAction("Save", {
                    onSave(initial.copy(title = title.trim().ifEmpty { "Result" }, date = date, category = category,
                        values = values.trim(), notes = notes.trim(), attachments = all), removed.toList())
                }, strong = true)
            },
        )
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(16.dp))
            Segmented(ResultCategory.entries.map { it.label }, category.ordinal) { category = ResultCategory.entries[it] }
            Spacer(Modifier.height(14.dp))
            FormGroup {
                FormField("Title", title, { title = it }, "e.g. Blood count, Morphology ultrasound")
                FormDivider()
                DateField("Date", date, { it?.let { d -> date = d } })
            }
            Spacer(Modifier.height(14.dp))
            FormGroup {
                FormField("Values", values, { values = it }, "One per line, e.g.\nHemoglobin: 12.1 g/dL\nGlucose: 82 mg/dL", multiline = true)
                FormDivider()
                FormField("Notes", notes, { notes = it }, "What the doctor said, next steps…", multiline = true)
            }
            SectionHeader("Files", action = {
                Box {
                    TextAction("Add", { menu = true }, strong = true)
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = P.Surface) {
                        DropdownMenuItem(text = { Text("Photo or screenshot") }, leadingIcon = { Icon(Icons.Rounded.PhotoLibrary, null) }, onClick = { menu = false; onPickPhoto() })
                        DropdownMenuItem(text = { Text("Take photo") }, leadingIcon = { Icon(Icons.Rounded.PhotoCamera, null) }, onClick = { menu = false; onTakePhoto() })
                        DropdownMenuItem(text = { Text("PDF or file") }, leadingIcon = { Icon(Icons.Rounded.PictureAsPdf, null) }, onClick = { menu = false; onPickFile() })
                    }
                }
            })
            if (all.isEmpty()) {
                Text("Attach photos of the report or the PDF from the lab.", color = P.Text3, fontSize = 13.sp, modifier = Modifier.padding(start = 4.dp))
            }
            all.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { a ->
                        AttachmentTile(a, thumbs, Modifier.weight(1f), onRemove = {
                            removed.add(a)
                            kept.remove(a)
                        }) {}
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

/** Shows a photo, or every page of a PDF, rendered on the phone. Pinch or double-tap to zoom. */
@Composable
fun AttachmentViewer(a: Attachment, render: suspend (Attachment) -> List<ImageBitmap>, onBack: () -> Unit, onShare: () -> Unit) {
    val pages by produceState<List<ImageBitmap>?>(null, a.id) { value = render(a) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    Column(Modifier.fillMaxSize().background(Color(0xFF1B1620))) {
        Row(Modifier.statusBarsPadding().fillMaxWidth().height(56.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.Close, "Close", tint = Color.White) }
            Text(a.name, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            IconButton(onClick = onShare) { Icon(Icons.Rounded.Share, "Share", tint = Color.White) }
        }
        Box(
            Modifier.fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 5f)
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
            contentAlignment = Alignment.Center,
        ) {
            val list = pages
            when {
                list == null -> CircularProgressIndicator(color = P.Rose)
                list.isEmpty() -> Text("Couldn’t open this file", color = Color.White)
                else -> LazyColumn(
                    Modifier.fillMaxSize().graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(list) { page ->
                        Image(page, null, contentScale = ContentScale.FillWidth, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp))
                    }
                }
            }
        }
    }
}
