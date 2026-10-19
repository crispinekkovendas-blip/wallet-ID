package com.prenatal.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prenatal.app.data.BabySize
import com.prenatal.app.data.Event
import com.prenatal.app.data.Gestation
import com.prenatal.app.data.Protocol
import com.prenatal.app.data.ProtocolItem
import com.prenatal.app.data.ProtocolStatus
import java.time.LocalDate
import java.time.LocalDateTime

// ---------------------------------------------------------------- Lock

@Composable
fun LockScreen(canAuthenticate: Boolean, error: String?, onUnlock: () -> Unit, onOpenSecuritySettings: () -> Unit) {
    Box(Modifier.fillMaxSize().background(P.HeroBrush)) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(Modifier.size(92.dp).clip(RoundedCornerShape(30.dp)).background(P.RoseBrush), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Favorite, null, tint = Color.White, modifier = Modifier.size(46.dp))
            }
            Spacer(Modifier.height(22.dp))
            Text("Prenatal", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = P.Text)
            Spacer(Modifier.height(10.dp))
            Text(
                if (canAuthenticate) "Your pregnancy records are private.\nUnlock with your fingerprint, face or phone PIN."
                else "Set up a screen lock (PIN, pattern, fingerprint or face) on this phone to protect your records.",
                color = P.Text2, fontSize = 15.sp, textAlign = TextAlign.Center, lineHeight = 21.sp,
            )
            Spacer(Modifier.height(32.dp))
            if (canAuthenticate) {
                PrimaryButton("Unlock", onUnlock, Modifier.fillMaxWidth(), icon = Icons.Rounded.Fingerprint)
            } else {
                PrimaryButton("Open security settings", onOpenSecuritySettings, Modifier.fillMaxWidth(), icon = Icons.Rounded.Lock)
            }
            if (error != null) {
                Spacer(Modifier.height(14.dp))
                Text(error, color = P.Bad, fontSize = 14.sp, textAlign = TextAlign.Center)
            }
        }
        Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Lock, null, tint = P.Text3, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
            Text("Encrypted · stored only on this phone", color = P.Text3, fontSize = 12.sp)
        }
    }
}

// ---------------------------------------------------------------- Setup

@Composable
fun SetupScreen(onDone: (due: LocalDate, mother: String, baby: String) -> Unit) {
    var useLmp by remember { mutableStateOf(false) }
    var date by remember { mutableStateOf<LocalDate?>(null) }
    var mother by remember { mutableStateOf("") }
    var baby by remember { mutableStateOf("") }
    val due = date?.let { if (useLmp) Gestation.dueDateFromLmp(it) else it }

    Column(
        Modifier.fillMaxSize().background(P.Bg).verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(28.dp))
        Text("🤰", fontSize = 52.sp)
        Spacer(Modifier.height(10.dp))
        Text("Welcome", style = androidx.compose.material3.MaterialTheme.typography.headlineLarge)
        Text("Let’s set up your pregnancy. Everything stays encrypted on this phone.", color = P.Text2, fontSize = 15.sp)

        SectionHeader("When is the baby due?")
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(P.Surface2).padding(4.dp)) {
            listOf(false to "I know the due date", true to "First day of last period").forEach { (lmp, label) ->
                Text(
                    label,
                    color = if (useLmp == lmp) Color.White else P.Text2,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(11.dp))
                        .background(if (useLmp == lmp) P.Rose else Color.Transparent)
                        .clickable { useLmp = lmp; date = null }
                        .padding(vertical = 10.dp),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        FormGroup {
            DateField(if (useLmp) "First day of last period (DUM)" else "Due date (DPP)", date, { date = it })
        }
        if (due != null) {
            val g = Gestation.fromDueDate(due)
            Spacer(Modifier.height(10.dp))
            Text(
                "Due ${due.format(BR_DATE)} · you’re ${g.label} today",
                color = P.RoseDeep, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 6.dp),
            )
        }

        SectionHeader("Names (optional)")
        FormGroup {
            FormField("Mother", mother, { mother = it }, "Your name")
            FormDivider()
            FormField("Baby", baby, { baby = it }, "If you’ve chosen one")
        }
        Spacer(Modifier.height(28.dp))
        PrimaryButton("Start", { due?.let { onDone(it, mother, baby) } }, Modifier.fillMaxWidth(), enabled = due != null)
        Spacer(Modifier.height(28.dp))
    }
}

// ---------------------------------------------------------------- Today

@Composable
fun HomeScreen(
    g: Gestation,
    motherName: String,
    babyName: String,
    events: List<Event>,
    skipped: Set<String>,
    now: LocalDateTime,
    onOpenEvent: (String) -> Unit,
    onOpenProtocol: (ProtocolItem) -> Unit,
    onAddEvent: () -> Unit,
    onAddResult: () -> Unit,
    onAddMeasurement: () -> Unit,
    onSeeSchedule: () -> Unit,
    onScan: () -> Unit = {},
) {
    val upcoming = events.filter { !it.done && it.at.isAfter(now.minusHours(2)) }.sortedBy { it.at }
    val dueNow = Protocol.items.filter { Protocol.status(it, g.weeks, events, skipped) in setOf(ProtocolStatus.DUE_NOW, ProtocolStatus.OVERDUE) }

    Column(Modifier.fillMaxSize().background(P.Bg).verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(16.dp))
        Text(
            if (motherName.isNotBlank()) "Hi, ${motherName.split(" ").first()} 💕" else "Hello 💕",
            color = P.Text2, fontSize = 15.sp,
        )
        Text(if (babyName.isNotBlank()) "Waiting for $babyName" else "Your pregnancy", style = androidx.compose.material3.MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(16.dp))

        // Hero: progress ring + week
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(P.HeroBrush).padding(22.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProgressRing(g.progress, Modifier.size(132.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${g.weeks}", color = P.RoseDeep, fontSize = 44.sp, fontWeight = FontWeight.Bold, lineHeight = 44.sp)
                        Text(if (g.dayOfWeek > 0) "weeks + ${g.dayOfWeek}d" else "weeks", color = P.Text2, fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.width(18.dp))
                Column {
                    Chip("Trimester ${g.trimester}", P.RoseDeep)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        when {
                            g.daysToGo > 1 -> "${g.daysToGo} days to go"
                            g.daysToGo == 1L -> "1 day to go"
                            g.daysToGo == 0L -> "Due today!"
                            else -> "${-g.daysToGo} days past due"
                        },
                        color = P.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    )
                    Text("Due ${g.dueDate.format(BR_DATE)}", color = P.Text2, fontSize = 14.sp)
                }
            }
        }

        BabySize.forWeek(g.weeks)?.let { (emoji, what) ->
            Spacer(Modifier.height(14.dp))
            SoftCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(emoji, fontSize = 40.sp)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("Baby is about the size of", color = P.Text2, fontSize = 13.sp)
                        Text(what, color = P.Text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Quick actions
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuickAction("Scan", Icons.Rounded.DocumentScanner, P.RoseDeep, onScan, Modifier.weight(1f))
            QuickAction("Visit", Icons.Rounded.Event, P.Rose, onAddEvent, Modifier.weight(1f))
            QuickAction("Result", Icons.Rounded.Science, P.Sky, onAddResult, Modifier.weight(1f))
            QuickAction("Weight", Icons.Rounded.MonitorWeight, P.Mint, onAddMeasurement, Modifier.weight(1f))
        }

        SectionHeader("Coming up", action = { TextAction("See all", onSeeSchedule, strong = true) })
        if (upcoming.isEmpty()) {
            SoftCard(onClick = onAddEvent) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBubble(Icons.Rounded.Add, P.Rose)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("No appointments yet", color = P.Text, fontWeight = FontWeight.SemiBold)
                        Text("Add your next visit or exam", color = P.Text2, fontSize = 14.sp)
                    }
                }
            }
        } else {
            upcoming.take(3).forEach { e ->
                EventRow(e, g.dueDate, now, onClick = { onOpenEvent(e.id) })
                Spacer(Modifier.height(10.dp))
            }
        }

        if (dueNow.isNotEmpty()) {
            SectionHeader("Checkups due now")
            dueNow.forEach { item ->
                ProtocolRow(item, Protocol.status(item, g.weeks, events, skipped), g, onClick = { onOpenProtocol(item) })
                Spacer(Modifier.height(10.dp))
            }
        }
        Text(
            Protocol.visitRhythm(g.weeks),
            color = P.Text3, fontSize = 13.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 120.dp),
        )
    }
}

@Composable
private fun QuickAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(P.Surface)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconBubble(icon, color, 40)
        Spacer(Modifier.height(8.dp))
        Text(label, color = P.Text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ProgressRing(progress: Float, modifier: Modifier, content: @Composable () -> Unit) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 12.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(Color.White.copy(alpha = 0.75f), -90f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            drawArc(
                Brush.sweepGradient(listOf(P.Peach, P.Rose, P.RoseDeep, P.Peach)),
                -90f, 360f * progress, false, Offset(inset, inset), arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        content()
    }
}

@Composable
fun EventRow(e: Event, due: LocalDate?, now: LocalDateTime, onClick: () -> Unit) {
    val color = P.kindColor(e.kind)
    SoftCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                Modifier.width(52.dp).clip(RoundedCornerShape(14.dp)).background(color.copy(alpha = 0.12f)).padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(e.at.dayOfMonth.toString(), color = color, fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 22.sp)
                Text(e.at.month.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH).uppercase(), color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(e.title, color = if (e.done) P.Text3 else P.Text, fontWeight = FontWeight.SemiBold, maxLines = 1)
                val parts = listOfNotNull(
                    e.at.format(TIME),
                    e.place.takeIf { it.isNotBlank() },
                    due?.let { "${Gestation.weeksAt(it, e.at.toLocalDate())}w" },
                )
                Text(parts.joinToString(" · "), color = P.Text2, fontSize = 13.sp, maxLines = 1)
            }
            when {
                e.done -> Chip("Done", P.Mint)
                e.at.toLocalDate() == now.toLocalDate() -> Chip("Today", P.Rose)
                e.at.toLocalDate() == now.toLocalDate().plusDays(1) -> Chip("Tomorrow", P.Amber)
                else -> Icon(Icons.Rounded.ChevronRight, null, tint = P.Text3)
            }
        }
    }
}

@Composable
fun ProtocolRow(item: ProtocolItem, status: ProtocolStatus, g: Gestation?, onClick: () -> Unit) {
    val (label, color) = when (status) {
        ProtocolStatus.DONE -> "Done" to P.Mint
        ProtocolStatus.SCHEDULED -> "Scheduled" to P.Sky
        ProtocolStatus.DUE_NOW -> "Due now" to P.Rose
        ProtocolStatus.OVERDUE -> "Overdue" to P.Bad
        ProtocolStatus.UPCOMING -> "Upcoming" to P.Text3
        ProtocolStatus.SKIPPED -> "Not needed" to P.Text3
    }
    SoftCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(item.title, color = if (status == ProtocolStatus.SKIPPED) P.Text3 else P.Text, fontWeight = FontWeight.SemiBold)
                Text(item.local, color = P.Text3, fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
                val window = if (item.toWeek >= 40) "from week ${item.fromWeek}" else "weeks ${item.fromWeek}–${item.toWeek}"
                val dates = g?.let { " · ${it.dateOfWeek(item.fromWeek).format(BR_DATE)}" }.orEmpty()
                Text(window + dates, color = P.Text2, fontSize = 13.sp)
            }
            Spacer(Modifier.width(10.dp))
            Chip(label, color)
        }
    }
}
