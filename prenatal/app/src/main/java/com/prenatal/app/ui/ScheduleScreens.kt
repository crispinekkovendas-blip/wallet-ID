package com.prenatal.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prenatal.app.data.Event
import com.prenatal.app.data.EventKind
import com.prenatal.app.data.Gestation
import com.prenatal.app.data.Protocol
import com.prenatal.app.data.ProtocolItem
import com.prenatal.app.data.ProtocolStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Composable
fun ScheduleScreen(
    g: Gestation?,
    events: List<Event>,
    skipped: Set<String>,
    now: LocalDateTime,
    onOpenEvent: (String) -> Unit,
    onOpenProtocol: (ProtocolItem) -> Unit,
    onAdd: () -> Unit,
    initialView: Int = 0,
) {
    var view by remember { mutableStateOf(initialView) } // 0 upcoming, 1 checkups, 2 past
    val upcoming = events.filter { !it.done && it.at.isAfter(now.minusHours(2)) }.sortedBy { it.at }
    val past = events.filter { it.done || !it.at.isAfter(now.minusHours(2)) }.sortedByDescending { it.at }

    Column(Modifier.fillMaxSize().background(P.Bg).verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Schedule", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
            PrimaryButton("Add", onAdd, icon = Icons.Rounded.Add)
        }
        Spacer(Modifier.height(16.dp))
        Segmented(listOf("Upcoming", "Checkups", "Past"), view) { view = it }
        Spacer(Modifier.height(14.dp))

        when (view) {
            0 -> if (upcoming.isEmpty()) {
                EmptyState("🗓️", "Nothing scheduled", "Add appointments, ultrasounds, lab tests and vaccines. You’ll get a reminder the day before and 2 hours before.")
            } else {
                upcoming.groupBy { it.at.format(MONTH_YEAR) }.forEach { (month, list) ->
                    Text(month, color = P.Text2, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
                    list.forEach { e ->
                        EventRow(e, g?.dueDate, now) { onOpenEvent(e.id) }
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
            1 -> {
                Text(
                    "A general guide based on the Brazilian Ministry of Health prenatal recommendations. Your doctor’s plan always comes first.",
                    color = P.Text2, fontSize = 13.sp, modifier = Modifier.padding(bottom = 12.dp),
                )
                Protocol.items.forEach { item ->
                    ProtocolRow(item, Protocol.status(item, g?.weeks ?: 0, events, skipped), g) { onOpenProtocol(item) }
                    Spacer(Modifier.height(10.dp))
                }
            }
            else -> if (past.isEmpty()) {
                EmptyState("🌷", "No past visits yet", "Completed appointments and exams will show here.")
            } else {
                past.forEach { e ->
                    EventRow(e, g?.dueDate, now) { onOpenEvent(e.id) }
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
        Spacer(Modifier.height(120.dp))
    }
}

@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(P.Surface2).padding(4.dp)) {
        options.forEachIndexed { i, label ->
            Text(
                label,
                color = if (i == selected) P.Text else P.Text2,
                fontWeight = if (i == selected) FontWeight.SemiBold else FontWeight.Medium,
                fontSize = 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (i == selected) P.Surface else Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(vertical = 9.dp),
            )
        }
    }
}

/** What to do with a suggested checkup. */
@Composable
fun ProtocolDialog(
    item: ProtocolItem,
    status: ProtocolStatus,
    g: Gestation?,
    onSchedule: () -> Unit,
    onDoneToday: () -> Unit,
    onSkip: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = P.Surface,
        title = { Text(item.title, color = P.Text) },
        text = {
            Column {
                Text(item.local, color = P.Text3, fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
                Text(item.details, color = P.Text2)
                Spacer(Modifier.height(10.dp))
                val window = if (item.toWeek >= 40) "From week ${item.fromWeek}" else "Weeks ${item.fromWeek}–${item.toWeek}"
                val dates = g?.let { " (${it.dateOfWeek(item.fromWeek).format(BR_DATE)} – ${it.dateOfWeek(minOf(item.toWeek, 40)).format(BR_DATE)})" }.orEmpty()
                Text(window + dates, color = P.RoseDeep, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (status != ProtocolStatus.DONE && status != ProtocolStatus.SKIPPED) {
                    TextButton(onClick = onSchedule) { Text("Schedule it", fontWeight = FontWeight.SemiBold) }
                    TextButton(onClick = onDoneToday) { Text("Mark as done today") }
                }
                if (status == ProtocolStatus.SKIPPED) {
                    TextButton(onClick = { onSkip(false) }) { Text("Show it again") }
                } else if (status != ProtocolStatus.DONE) {
                    TextButton(onClick = { onSkip(true) }) { Text("Not needed for me", color = P.Text2) }
                }
                TextButton(onClick = onDismiss) { Text("Close", color = P.Text2) }
            }
        },
    )
}

@Composable
fun EventEditScreen(
    initial: Event,
    isNew: Boolean,
    g: Gestation?,
    onCancel: () -> Unit,
    onSave: (Event) -> Unit,
    onDelete: () -> Unit,
    onAddResult: () -> Unit,
) {
    var kind by remember { mutableStateOf(initial.kind) }
    var title by remember { mutableStateOf(initial.title) }
    var date by remember { mutableStateOf(initial.at.toLocalDate()) }
    var time by remember { mutableStateOf(initial.at.toLocalTime()) }
    var place by remember { mutableStateOf(initial.place) }
    var pro by remember { mutableStateOf(initial.professional) }
    var notes by remember { mutableStateOf(initial.notes) }
    var done by remember { mutableStateOf(initial.done) }
    var remind by remember { mutableStateOf(initial.remind) }
    var confirmDelete by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(P.Bg).imePadding()) {
        TopBar(
            if (isNew) "New ${kind.label.lowercase()}" else kind.label,
            left = { TextAction("Cancel", onCancel) },
            right = {
                TextAction("Save", {
                    onSave(initial.copy(kind = kind, title = title.trim().ifEmpty { kind.label }, at = date.atTime(time),
                        place = place.trim(), professional = pro.trim(), notes = notes.trim(), done = done, remind = remind))
                }, strong = true)
            },
        )
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(16.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EventKind.entries.forEach { k ->
                    val c = P.kindColor(k)
                    Text(
                        k.label,
                        color = if (k == kind) Color.White else c,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (k == kind) c else c.copy(alpha = 0.1f))
                            .clickable { kind = k }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            FormGroup {
                FormField("Title", title, { title = it }, "e.g. Prenatal visit, Morphology ultrasound")
                FormDivider()
                DateField("Date", date, { it?.let { d -> date = d } })
                FormDivider()
                TimeField("Time", time) { time = it }
            }
            if (g != null) {
                Text(
                    "You’ll be ${Gestation.weeksAt(g.dueDate, date)} weeks on this day",
                    color = P.RoseDeep, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 6.dp, top = 8.dp),
                )
            }
            Spacer(Modifier.height(14.dp))
            FormGroup {
                FormField("Place", place, { place = it }, "Clinic, hospital or lab")
                FormDivider()
                FormField("Doctor / professional", pro, { pro = it })
                FormDivider()
                FormField("Notes", notes, { notes = it }, "Questions to ask, preparation (fasting…)", multiline = true)
            }
            Spacer(Modifier.height(14.dp))
            FormGroup {
                SwitchRow("Remind me", "The day before at 9:00 and 2 hours before", remind) { remind = it }
                FormDivider()
                SwitchRow("Done", "Mark when the visit or exam has happened", done) { done = it }
            }
            if (!isNew) {
                Spacer(Modifier.height(14.dp))
                PrimaryButton("Add a result for this", onAddResult, Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
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
        ConfirmDialog("Delete this?", "It will be removed from your schedule.", "Delete", danger = true,
            onConfirm = { confirmDelete = false; onDelete() }, onDismiss = { confirmDelete = false })
    }
}

@Composable
fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = P.Text, fontSize = 16.sp)
            Text(subtitle, color = P.Text3, fontSize = 12.sp)
        }
        Switch(checked, onChange, colors = SwitchDefaults.colors(checkedTrackColor = P.Rose))
    }
}

@Composable
fun ConfirmDialog(title: String, text: String, confirm: String, danger: Boolean = false, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = P.Surface,
        title = { Text(title, color = P.Text) },
        text = { Text(text, color = P.Text2) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirm, color = if (danger) P.Bad else P.Rose, fontWeight = FontWeight.SemiBold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = P.Text2) } },
    )
}

/** A new event, defaulting to a sensible time and, for a suggested checkup, its details. */
fun draftEvent(item: ProtocolItem?, g: Gestation?): Event {
    val start = g?.let { maxOf(it.dateOfWeek(item?.fromWeek ?: 0), LocalDate.now()) } ?: LocalDate.now()
    return Event(
        kind = item?.kind ?: EventKind.APPOINTMENT,
        title = item?.title.orEmpty(),
        at = (if (item == null) LocalDate.now().plusDays(1) else start).atTime(LocalTime.of(9, 0)),
        notes = item?.let { "${it.local}\n${it.details}" }.orEmpty(),
        protocolKey = item?.key,
    )
}
