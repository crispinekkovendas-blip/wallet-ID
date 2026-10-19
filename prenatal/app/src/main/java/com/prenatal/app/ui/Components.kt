package com.prenatal.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

val BR_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
val DAY_MONTH: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH)
val MONTH_YEAR: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)

/** A white rounded card with a soft shadow. */
@Composable
fun SoftCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(22.dp), ambientColor = Color(0x22C94467), spotColor = Color(0x22C94467))
            .clip(RoundedCornerShape(22.dp))
            .background(P.Surface)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(18.dp),
        content = content,
    )
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = 24.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, color = P.Text, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

@Composable
fun TopBar(title: String, left: @Composable RowScope.() -> Unit = {}, right: @Composable RowScope.() -> Unit = {}) {
    Column(Modifier.background(P.Bg).statusBarsPadding()) {
        Box(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp)) {
            Row(Modifier.align(Alignment.CenterStart), verticalAlignment = Alignment.CenterVertically, content = left)
            Text(
                title,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = P.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).padding(horizontal = 88.dp),
            )
            Row(Modifier.align(Alignment.CenterEnd), verticalAlignment = Alignment.CenterVertically, content = right)
        }
        HorizontalDivider(color = P.Line)
    }
}

@Composable
fun TextAction(text: String, onClick: () -> Unit, strong: Boolean = false, enabled: Boolean = true) {
    Text(
        text,
        color = when {
            !enabled -> P.Text3
            strong -> P.Rose
            else -> P.Text2
        },
        fontSize = 16.sp,
        fontWeight = if (strong) FontWeight.SemiBold else FontWeight.Normal,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    )
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true) {
    Row(
        modifier
            .heightIn(min = 52.dp)
            .shadow(if (enabled) 12.dp else 0.dp, RoundedCornerShape(16.dp), spotColor = P.Rose, ambientColor = P.Rose)
            .clip(RoundedCornerShape(16.dp))
            .background(P.RoseBrush, alpha = if (enabled) 1f else 0.4f)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 22.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}

@Composable
fun Chip(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text,
        color = color,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
fun IconBubble(icon: ImageVector, color: Color, size: Int = 44) {
    Box(
        Modifier.size(size.dp).clip(RoundedCornerShape((size * 0.32).dp)).background(color.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size((size * 0.5).dp))
    }
}

/** A labelled text input in the grouped form style. */
@Composable
fun FormField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    placeholder: String = "",
    multiline: Boolean = false,
    keyboard: KeyboardType = KeyboardType.Text,
    capitalize: Boolean = true,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(label, color = P.Text3, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(3.dp))
        Box {
            if (value.isEmpty() && placeholder.isNotEmpty()) Text(placeholder, color = P.Text3.copy(alpha = 0.7f), fontSize = 17.sp)
            BasicTextField(
                value,
                onChange,
                singleLine = !multiline,
                minLines = if (multiline) 3 else 1,
                textStyle = TextStyle(color = P.Text, fontSize = 17.sp),
                cursorBrush = SolidColor(P.Rose),
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboard,
                    capitalization = if (capitalize) KeyboardCapitalization.Sentences else KeyboardCapitalization.None,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun FormGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(P.Surface)
            .border(1.dp, P.Line, RoundedCornerShape(20.dp)),
        content = content,
    )
}

@Composable
fun FormDivider() = HorizontalDivider(color = P.Line, modifier = Modifier.padding(start = 16.dp))

@Composable
fun ClickRow(label: String, value: String, icon: ImageVector, onClick: () -> Unit, onClear: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, color = P.Text3, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(3.dp))
            Text(value.ifEmpty { "Select" }, color = if (value.isEmpty()) P.Text3 else P.Text, fontSize = 17.sp)
        }
        if (onClear != null && value.isNotEmpty()) {
            Icon(Icons.Rounded.Close, "Clear", tint = P.Text3, modifier = Modifier.size(20.dp).clip(CircleShape).clickable(onClick = onClear))
            Spacer(Modifier.width(10.dp))
        }
        Icon(icon, null, tint = P.Text3, modifier = Modifier.size(20.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(label: String, date: LocalDate?, onPick: (LocalDate?) -> Unit, clearable: Boolean = false) {
    var open by remember { mutableStateOf(false) }
    ClickRow(label, date?.format(BR_DATE).orEmpty(), Icons.Rounded.CalendarMonth, { open = true }, if (clearable) ({ onPick(null) }) else null)
    if (open) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (date ?: LocalDate.now()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    open = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancel") } },
        ) { DatePicker(state) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeField(label: String, time: LocalTime, onPick: (LocalTime) -> Unit) {
    var open by remember { mutableStateOf(false) }
    ClickRow(label, time.format(TIME), Icons.Rounded.Schedule, { open = true })
    if (open) {
        val state = rememberTimePickerState(initialHour = time.hour, initialMinute = time.minute, is24Hour = true)
        Dialog(onDismissRequest = { open = false }) {
            Column(
                Modifier.clip(RoundedCornerShape(28.dp)).background(P.Surface).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Time", fontWeight = FontWeight.SemiBold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 16.dp))
                TimePicker(state)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { open = false }) { Text("Cancel") }
                    TextButton(onClick = { onPick(LocalTime.of(state.hour, state.minute)); open = false }) { Text("OK") }
                }
            }
        }
    }
}

@Composable
fun BusyOverlay(text: String) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)).clickable(enabled = true, onClick = {}), contentAlignment = Alignment.Center) {
        Column(
            Modifier.clip(RoundedCornerShape(22.dp)).background(P.Surface).padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator(color = P.Rose, strokeWidth = 3.dp, modifier = Modifier.size(34.dp))
            Spacer(Modifier.height(14.dp))
            Text(text, color = P.Text2)
        }
    }
}

@Composable
fun EmptyState(emoji: String, title: String, text: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(vertical = 36.dp, horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 44.sp)
        Spacer(Modifier.height(10.dp))
        Text(title, color = P.Text, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
        Spacer(Modifier.height(4.dp))
        Text(text, color = P.Text2, fontSize = 14.sp, textAlign = TextAlign.Center)
    }
}

/** "Fill from a photo": camera or gallery, read on the phone. */
@Composable
fun ScanCard(title: String, subtitle: String, onCamera: () -> Unit, onGallery: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(P.HeroBrush)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBubble(Icons.Rounded.DocumentScanner, P.RoseDeep, 44)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = P.Text, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = P.Text2, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ScanButton("Camera", Icons.Rounded.PhotoCamera, onCamera, Modifier.weight(1f))
            ScanButton("Gallery", Icons.Rounded.PhotoLibrary, onGallery, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ScanButton(label: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(14.dp)).background(P.Surface).clickable(onClick = onClick).padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = P.RoseDeep, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = P.Text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

/** The raw text read from the photo, so anything the form missed can be copied. */
@Composable
fun RecognizedText(text: String) {
    var open by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(P.Surface).border(1.dp, P.Line, RoundedCornerShape(18.dp))
            .clickable { open = !open }.padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Recognized text", color = P.Text2, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Text(if (open) "Hide" else "Show", color = P.Rose, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        if (open) {
            Spacer(Modifier.height(8.dp))
            androidx.compose.foundation.text.selection.SelectionContainer {
                Text(text, color = P.Text, fontSize = 13.sp, lineHeight = 18.sp)
            }
        }
    }
}

/** What to scan, from the Today screen. */
@Composable
fun ScanChooser(onResult: (camera: Boolean) -> Unit, onAppointment: (camera: Boolean) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.clip(RoundedCornerShape(28.dp)).background(P.Surface).padding(20.dp)) {
            Text("Scan a document", color = P.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("It’s read here on your phone; nothing is uploaded.", color = P.Text2, fontSize = 13.sp)
            Spacer(Modifier.height(16.dp))
            ScanCard("Test result", "Lab report or ultrasound: values, date and the photo", { onResult(true) }, { onResult(false) })
            Spacer(Modifier.height(12.dp))
            ScanCard("Appointment", "Slip, exam request or WhatsApp message: date, time and place", { onAppointment(true) }, { onAppointment(false) })
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = onDismiss) { Text("Close") } }
        }
    }
}
