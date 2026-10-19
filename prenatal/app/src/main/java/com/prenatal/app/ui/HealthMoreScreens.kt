package com.prenatal.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.Luggage
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prenatal.app.data.CheckItem
import com.prenatal.app.data.Gestation
import com.prenatal.app.data.Measurement
import com.prenatal.app.data.Profile
import java.time.LocalDate

private fun Double.fmt(): String = if (this % 1.0 == 0.0) toInt().toString() else String.format(java.util.Locale.US, "%.1f", this)

// ---------------------------------------------------------------- Health

@Composable
fun HealthScreen(measurements: List<Measurement>, due: LocalDate?, onAdd: () -> Unit, onOpen: (String) -> Unit) {
    val sorted = measurements.sortedBy { it.date }
    val weights = sorted.filter { it.weightKg != null }
    val lastWeight = weights.lastOrNull()
    val lastBp = sorted.lastOrNull { it.bpSystolic != null && it.bpDiastolic != null }
    val gain = if (weights.size >= 2) weights.last().weightKg!! - weights.first().weightKg!! else null

    Column(Modifier.fillMaxSize().background(P.Bg).verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Health", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
            PrimaryButton("Log", onAdd, icon = Icons.Rounded.Add)
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("Weight", lastWeight?.weightKg?.let { "${it.fmt()} kg" } ?: "—",
                gain?.let { (if (it >= 0) "+" else "") + it.fmt() + " kg total" } ?: "Log your weight", P.Rose, Modifier.weight(1f))
            StatTile("Blood pressure", lastBp?.let { "${it.bpSystolic}/${it.bpDiastolic}" } ?: "—",
                lastBp?.date?.format(BR_DATE) ?: "mmHg", P.Lavender, Modifier.weight(1f))
        }
        if (weights.size >= 2) {
            SectionHeader("Weight over the weeks")
            SoftCard { WeightChart(weights, due, Modifier.fillMaxWidth().height(170.dp)) }
        }
        SectionHeader("Log")
        if (sorted.isEmpty()) {
            EmptyState("⚖️", "Nothing logged yet", "Track weight, blood pressure, fundal height and the baby’s heart rate from each visit.")
        }
        sorted.reversed().forEach { m ->
            SoftCard(onClick = { onOpen(m.id) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        val wk = due?.let { " · ${Gestation.weeksAt(it, m.date)}w" }.orEmpty()
                        Text(m.date.format(BR_DATE) + wk, color = P.Text, fontWeight = FontWeight.SemiBold)
                        val parts = listOfNotNull(
                            m.weightKg?.let { "${it.fmt()} kg" },
                            if (m.bpSystolic != null && m.bpDiastolic != null) "BP ${m.bpSystolic}/${m.bpDiastolic}" else null,
                            m.fundalCm?.let { "Fundal ${it.fmt()} cm" },
                            m.fetalHr?.let { "Baby ♥ $it bpm" },
                        )
                        Text(parts.joinToString(" · ").ifEmpty { m.notes }, color = P.Text2, fontSize = 13.sp)
                    }
                    Icon(Icons.Rounded.ChevronRight, null, tint = P.Text3)
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(120.dp))
    }
}

@Composable
private fun StatTile(label: String, value: String, sub: String, color: Color, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(22.dp)).background(P.Surface).padding(16.dp)) {
        Text(label, color = P.Text2, fontSize = 13.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = color, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text(sub, color = P.Text3, fontSize = 12.sp)
    }
}

@Composable
private fun WeightChart(points: List<Measurement>, due: LocalDate?, modifier: Modifier) {
    val xs = points.map { m -> due?.let { Gestation.weeksAt(it, m.date).toFloat() } ?: m.date.toEpochDay().toFloat() }
    val ys = points.map { it.weightKg!!.toFloat() }
    val minY = ys.min() - 1f
    val maxY = ys.max() + 1f
    val minX = xs.min()
    val maxX = maxOf(xs.max(), minX + 1f)
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().weight(1f)) {
            fun pt(i: Int) = Offset(
                (xs[i] - minX) / (maxX - minX) * size.width,
                size.height - (ys[i] - minY) / (maxY - minY) * size.height,
            )
            for (k in 0..3) {
                val y = size.height * k / 3f
                drawLine(P.Line, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
            }
            val line = Path().apply { points.indices.forEach { i -> if (i == 0) moveTo(pt(i).x, pt(i).y) else lineTo(pt(i).x, pt(i).y) } }
            val fill = Path().apply {
                addPath(line)
                lineTo(pt(points.lastIndex).x, size.height)
                lineTo(pt(0).x, size.height)
                close()
            }
            drawPath(fill, Brush.verticalGradient(listOf(P.Rose.copy(alpha = 0.25f), Color.Transparent)))
            drawPath(line, P.Rose, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            points.indices.forEach { i ->
                drawCircle(Color.White, 5.dp.toPx(), pt(i))
                drawCircle(P.Rose, 3.5.dp.toPx(), pt(i))
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            Text(if (due != null) "Week ${minX.toInt()}" else points.first().date.format(BR_DATE), color = P.Text3, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Text("${ys.min().toDouble().fmt()}–${ys.max().toDouble().fmt()} kg", color = P.Text3, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Text(if (due != null) "Week ${maxX.toInt()}" else points.last().date.format(BR_DATE), color = P.Text3, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
        }
    }
}

@Composable
fun MeasurementEditScreen(initial: Measurement, isNew: Boolean, onCancel: () -> Unit, onSave: (Measurement) -> Unit, onDelete: () -> Unit) {
    var date by remember { mutableStateOf(initial.date) }
    var weight by remember { mutableStateOf(initial.weightKg?.fmt().orEmpty()) }
    var sys by remember { mutableStateOf(initial.bpSystolic?.toString().orEmpty()) }
    var dia by remember { mutableStateOf(initial.bpDiastolic?.toString().orEmpty()) }
    var fundal by remember { mutableStateOf(initial.fundalCm?.fmt().orEmpty()) }
    var hr by remember { mutableStateOf(initial.fetalHr?.toString().orEmpty()) }
    var notes by remember { mutableStateOf(initial.notes) }
    var confirmDelete by remember { mutableStateOf(false) }
    fun num(s: String) = s.replace(',', '.').trim().toDoubleOrNull()

    Column(Modifier.fillMaxSize().background(P.Bg).imePadding()) {
        TopBar(
            if (isNew) "Log measurements" else "Measurements",
            left = { TextAction("Cancel", onCancel) },
            right = {
                TextAction("Save", {
                    onSave(initial.copy(date = date, weightKg = num(weight), bpSystolic = num(sys)?.toInt(), bpDiastolic = num(dia)?.toInt(),
                        fundalCm = num(fundal), fetalHr = num(hr)?.toInt(), notes = notes.trim()))
                }, strong = true)
            },
        )
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(16.dp))
            FormGroup { DateField("Date", date, { it?.let { d -> date = d } }) }
            Spacer(Modifier.height(14.dp))
            FormGroup {
                FormField("Weight (kg)", weight, { weight = it }, "e.g. 64.5", keyboard = KeyboardType.Decimal)
                FormDivider()
                Row {
                    Box(Modifier.weight(1f)) { FormField("BP systolic", sys, { sys = it }, "e.g. 110", keyboard = KeyboardType.Number) }
                    Box(Modifier.weight(1f)) { FormField("BP diastolic", dia, { dia = it }, "e.g. 70", keyboard = KeyboardType.Number) }
                }
                FormDivider()
                FormField("Fundal height (cm)", fundal, { fundal = it }, "Altura uterina", keyboard = KeyboardType.Decimal)
                FormDivider()
                FormField("Baby’s heart rate (bpm)", hr, { hr = it }, "e.g. 145", keyboard = KeyboardType.Number)
                FormDivider()
                FormField("Notes", notes, { notes = it }, multiline = true)
            }
            if (!isNew) {
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).border(1.dp, P.Line, RoundedCornerShape(16.dp))
                        .clickable { confirmDelete = true }.padding(14.dp),
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.DeleteOutline, null, tint = P.Bad)
                    Spacer(Modifier.width(8.dp))
                    Text("Delete", color = P.Bad, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
    if (confirmDelete) {
        ConfirmDialog("Delete this entry?", "", "Delete", danger = true, onConfirm = { confirmDelete = false; onDelete() }, onDismiss = { confirmDelete = false })
    }
}

// ---------------------------------------------------------------- More

@Composable
fun MoreScreen(
    profile: Profile,
    bag: List<CheckItem>,
    lockAfterSeconds: Int,
    version: String,
    onEditProfile: () -> Unit,
    onOpenBag: () -> Unit,
    onLockAfter: (Int) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onErase: () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(P.Bg).verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(16.dp))
        Text("More", style = MaterialTheme.typography.headlineLarge)

        SectionHeader("My information")
        SoftCard(onClick = onEditProfile) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBubble(Icons.Rounded.Person, P.Rose)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(profile.motherName.ifEmpty { "Your details" }, color = P.Text, fontWeight = FontWeight.SemiBold)
                    val parts = listOfNotNull(
                        profile.bloodType.takeIf { it.isNotBlank() }?.let { "Blood type $it" },
                        profile.dueDate?.let { "Due ${it.format(BR_DATE)}" },
                    )
                    Text(parts.joinToString(" · ").ifEmpty { "Blood type, doctor, hospital" }, color = P.Text2, fontSize = 13.sp)
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = P.Text3)
            }
            if (profile.doctor.isNotBlank() || profile.hospital.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                if (profile.doctor.isNotBlank()) InfoLine(Icons.Rounded.Favorite, profile.doctor, profile.doctorPhone)
                if (profile.hospital.isNotBlank()) InfoLine(Icons.Rounded.LocalHospital, profile.hospital, profile.hospitalPhone)
            }
        }
        Spacer(Modifier.height(10.dp))
        SoftCard(onClick = onOpenBag) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBubble(Icons.Rounded.Luggage, P.Lavender)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Hospital bag", color = P.Text, fontWeight = FontWeight.SemiBold)
                    Text("${bag.count { it.done }} of ${bag.size} packed", color = P.Text2, fontSize = 13.sp)
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = P.Text3)
            }
        }

        SectionHeader("Security")
        FormGroup {
            listOf(0 to "Lock immediately when I leave", 60 to "Lock after 1 minute", 300 to "Lock after 5 minutes").forEachIndexed { i, (secs, label) ->
                if (i > 0) FormDivider()
                Row(Modifier.fillMaxWidth().clickable { onLockAfter(secs) }.padding(start = 16.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Schedule, null, tint = P.Rose, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(14.dp))
                    Text(label, color = P.Text, modifier = Modifier.weight(1f))
                    RadioButton(lockAfterSeconds == secs, { onLockAfter(secs) }, colors = RadioButtonDefaults.colors(selectedColor = P.Rose))
                }
            }
        }

        SectionHeader("Backup")
        FormGroup {
            MoreRow(Icons.Rounded.Download, "Export encrypted backup", onExport)
            FormDivider()
            MoreRow(Icons.Rounded.Upload, "Restore from backup", onImport)
        }
        Text(
            "Your records are encrypted with a key kept inside this phone. Keep a backup somewhere safe: it’s the only way to restore them on a new phone.",
            color = P.Text3, fontSize = 13.sp, modifier = Modifier.padding(start = 6.dp, top = 8.dp, end = 6.dp),
        )
        Spacer(Modifier.height(20.dp))
        FormGroup { MoreRow(Icons.Rounded.DeleteOutline, "Erase all data", onErase, danger = true) }
        Text("Prenatal $version", color = P.Text3, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 120.dp))
    }
}

@Composable
private fun InfoLine(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, phone: String) {
    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = P.Text3, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(text + if (phone.isNotBlank()) " · $phone" else "", color = P.Text2, fontSize = 14.sp)
    }
}

@Composable
private fun MoreRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClick: () -> Unit, danger: Boolean = false) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = if (danger) P.Bad else P.Rose, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Text(text, color = if (danger) P.Bad else P.Text, modifier = Modifier.weight(1f))
    }
}

@Composable
fun ProfileEditScreen(initial: Profile, onCancel: () -> Unit, onSave: (Profile) -> Unit) {
    var p by remember { mutableStateOf(initial) }
    Column(Modifier.fillMaxSize().background(P.Bg).imePadding()) {
        TopBar("My information", left = { TextAction("Cancel", onCancel) }, right = { TextAction("Save", { onSave(p) }, strong = true) })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 20.dp)) {
            SectionHeader("Pregnancy")
            FormGroup {
                FormField("Mother", p.motherName, { p = p.copy(motherName = it) })
                FormDivider()
                FormField("Baby", p.babyName, { p = p.copy(babyName = it) }, "If you’ve chosen a name")
                FormDivider()
                DateField("Due date (DPP)", p.dueDate, { it?.let { d -> p = p.copy(dueDate = d) } })
                FormDivider()
                FormField("Blood type", p.bloodType, { p = p.copy(bloodType = it) }, "e.g. O+")
            }
            SectionHeader("Care team")
            FormGroup {
                FormField("Doctor / midwife", p.doctor, { p = p.copy(doctor = it) })
                FormDivider()
                FormField("Phone", p.doctorPhone, { p = p.copy(doctorPhone = it) }, keyboard = KeyboardType.Phone)
                FormDivider()
                FormField("Hospital / maternity", p.hospital, { p = p.copy(hospital = it) })
                FormDivider()
                FormField("Phone", p.hospitalPhone, { p = p.copy(hospitalPhone = it) }, keyboard = KeyboardType.Phone)
            }
            SectionHeader("Notes")
            FormGroup { FormField("Allergies, medicines, birth plan…", p.notes, { p = p.copy(notes = it) }, multiline = true) }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
fun BagScreen(items: List<CheckItem>, onBack: () -> Unit, onChange: (List<CheckItem>) -> Unit) {
    var newItem by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(P.Bg).imePadding()) {
        TopBar("Hospital bag", left = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBackIos, "Back", tint = P.Text, modifier = Modifier.size(20.dp)) }
        })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(16.dp))
            val done = items.count { it.done }
            Text("$done of ${items.size} packed", color = P.RoseDeep, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            FormGroup {
                items.forEachIndexed { i, item ->
                    if (i > 0) FormDivider()
                    Row(Modifier.fillMaxWidth().clickable { onChange(items.map { if (it.id == item.id) it.copy(done = !it.done) else it }) }
                        .padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (item.done) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked, null,
                            tint = if (item.done) P.Mint else P.Text3, modifier = Modifier.size(24.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            item.text, color = if (item.done) P.Text3 else P.Text, modifier = Modifier.weight(1f),
                            textDecoration = if (item.done) TextDecoration.LineThrough else null,
                        )
                        Icon(Icons.Rounded.Close, "Remove", tint = P.Text3, modifier = Modifier.size(18.dp).clip(CircleShape)
                            .clickable { onChange(items.filterNot { it.id == item.id }) })
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(P.Surface).border(1.dp, P.Line, RoundedCornerShape(16.dp))
                    .padding(start = 16.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f).padding(vertical = 14.dp)) {
                    if (newItem.isEmpty()) Text("Add an item", color = P.Text3)
                    BasicTextField(newItem, { newItem = it }, singleLine = true, textStyle = TextStyle(color = P.Text, fontSize = 16.sp),
                        cursorBrush = SolidColor(P.Rose), modifier = Modifier.fillMaxWidth())
                }
                TextAction("Add", {
                    if (newItem.isNotBlank()) {
                        onChange(items + CheckItem(text = newItem.trim()))
                        newItem = ""
                    }
                }, strong = true)
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

/** Passphrase prompt for backups. With [repeat] it asks twice and needs 8+ characters. */
@Composable
fun PassphraseDialog(title: String, text: String, confirm: String, repeat: Boolean, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var p1 by remember { mutableStateOf("") }
    var p2 by remember { mutableStateOf("") }
    val ok = if (repeat) p1.length >= 8 && p1 == p2 else p1.isNotEmpty()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = P.Surface,
        icon = { Icon(Icons.Rounded.Key, null, tint = P.Rose) },
        title = { Text(title) },
        text = {
            Column {
                Text(text, color = P.Text2, fontSize = 14.sp)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(p1, { p1 = it }, label = { Text("Passphrase") }, singleLine = true, visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = P.Rose), modifier = Modifier.fillMaxWidth())
                if (repeat) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(p2, { p2 = it }, label = { Text("Repeat passphrase") }, singleLine = true, visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = P.Rose), modifier = Modifier.fillMaxWidth())
                    if (p1.isNotEmpty() && p1.length < 8) Text("Use at least 8 characters", color = P.Bad, fontSize = 13.sp)
                    else if (p2.isNotEmpty() && p1 != p2) Text("Passphrases don’t match", color = P.Bad, fontSize = 13.sp)
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(p1) }, enabled = ok) { Text(confirm, fontWeight = FontWeight.SemiBold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = P.Text2) } },
    )
}

@Composable
fun EraseDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    var typed by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = P.Surface,
        title = { Text("Erase all data?") },
        text = {
            Column {
                Text("Every appointment, result, file and measurement will be deleted from this phone. Type ERASE to confirm.", color = P.Text2)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(typed, { typed = it }, singleLine = true, placeholder = { Text("ERASE") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = typed.trim().equals("ERASE", ignoreCase = true)) {
                Text("Erase everything", color = P.Bad, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = P.Text2) } },
    )
}
