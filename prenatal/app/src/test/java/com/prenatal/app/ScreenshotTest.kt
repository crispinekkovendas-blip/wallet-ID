package com.prenatal.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.prenatal.app.data.Attachment
import com.prenatal.app.data.Event
import com.prenatal.app.data.EventKind
import com.prenatal.app.data.Gestation
import com.prenatal.app.data.Measurement
import com.prenatal.app.data.Profile
import com.prenatal.app.data.Result
import com.prenatal.app.data.ResultCategory
import com.prenatal.app.data.defaultBag
import com.prenatal.app.ui.BagScreen
import com.prenatal.app.ui.HealthScreen
import com.prenatal.app.ui.HomeScreen
import com.prenatal.app.ui.LockScreen
import com.prenatal.app.ui.MoreScreen
import com.prenatal.app.ui.PrenatalTheme
import com.prenatal.app.ui.ResultViewScreen
import com.prenatal.app.ui.ResultsScreen
import com.prenatal.app.ui.ScheduleScreen
import com.prenatal.app.ui.SetupScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Every main screen with made-up sample data, for reviewing the design without a phone. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h852dp-xhdpi")
class ScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private val dir = File(System.getProperty("screenshots.dir") ?: "build/screenshots").apply { mkdirs() }
    private fun shot(name: String, content: @Composable () -> Unit) {
        compose.setContent { PrenatalTheme(content) }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage(File(dir, "$name.png").absolutePath)
    }

    private val today = LocalDate.now()
    private val now = LocalDateTime.of(today, LocalTime.of(10, 0))
    private val due = today.plusDays(280L - (24 * 7 + 3))
    private val g = Gestation.fromDueDate(due, today)
    private val events = listOf(
        Event(kind = EventKind.APPOINTMENT, title = "Prenatal visit", at = now.plusDays(1).withHour(14).withMinute(30), place = "Clínica Bem Nascer"),
        Event(kind = EventKind.LAB, title = "Glucose tolerance test", at = now.plusDays(6).withHour(7).withMinute(30), place = "Lab Central", protocolKey = "ogtt"),
        Event(kind = EventKind.VACCINE, title = "dTpa vaccine", at = now.plusDays(12).withHour(9).withMinute(0), place = "UBS"),
        Event(kind = EventKind.ULTRASOUND, title = "Morphology ultrasound", at = now.minusDays(10), done = true, protocolKey = "us-morph"),
        Event(kind = EventKind.ULTRASOUND, title = "Nuchal translucency", at = now.minusDays(70), done = true, protocolKey = "us-nt"),
        Event(kind = EventKind.LAB, title = "1st trimester labs", at = now.minusDays(90), done = true, protocolKey = "labs-1"),
        Event(kind = EventKind.APPOINTMENT, title = "First visit", at = now.minusDays(100), done = true, protocolKey = "first-visit"),
    )
    private val att = Attachment(name = "morfologico.pdf", mime = "application/pdf")
    private val results = listOf(
        Result(title = "Morphology ultrasound", date = today.minusDays(10), category = ResultCategory.ULTRASOUND,
            values = "Estimated weight: 640 g\nHeart rate: 148 bpm\nPlacenta: posterior, grade 0\nAmniotic fluid: normal",
            notes = "Everything looks great. Baby is active!", attachments = listOf(att)),
        Result(title = "Blood count", date = today.minusDays(90), category = ResultCategory.LAB, values = "Hemoglobin: 12.1 g/dL\nPlatelets: 245,000"),
        Result(title = "Urine culture", date = today.minusDays(90), category = ResultCategory.LAB, values = "Negative"),
    )
    private val measurements = (0..6).map { i ->
        Measurement(date = today.minusDays(21L * (6 - i)), weightKg = 61.0 + i * 0.9 + (i % 2) * 0.2, bpSystolic = 108 + i % 3, bpDiastolic = 68 + i % 2, fetalHr = 140 + i)
    }
    private val sampleReport = Bitmap.createBitmap(600, 800, Bitmap.Config.ARGB_8888).apply {
        val c = Canvas(this)
        c.drawColor(-1)
        val p = Paint().apply { color = 0xFFDDDDDD.toInt() }
        for (i in 0 until 14) c.drawRect(40f, 60f + i * 50f, 560f - (i % 3) * 80f, 80f + i * 50f, p)
    }.asImageBitmap()

    @Test fun lock() = shot("01-lock") { LockScreen(true, null, {}, {}) }
    @Test fun setup() = shot("02-setup") { SetupScreen { _, _, _ -> } }
    @Test fun home() = shot("03-today") {
        HomeScreen(g, "Ana Souza", "Lucas", events, emptySet(), now, {}, {}, {}, {}, {}, {})
    }
    @Test fun schedule() = shot("04-schedule") { ScheduleScreen(g, events, emptySet(), now, {}, {}, {}) }
    @Test fun checkups() = shot("05-checkups") { ScheduleScreen(g, events, setOf("anti-d"), now, {}, {}, {}, initialView = 1) }
    @Test fun resultsList() = shot("06-results") { ResultsScreen(results, due, { sampleReport }, {}, {}) }
    @Test fun resultView() = shot("07-result") { ResultViewScreen(results[0], due, { sampleReport }, {}, {}, {}, {}) }
    @Test fun health() = shot("08-health") { HealthScreen(measurements, due, {}, {}) }
    @Test fun more() = shot("09-more") {
        MoreScreen(
            Profile(motherName = "Ana Souza", dueDate = due, bloodType = "O+", doctor = "Dra. Marina Lopes", doctorPhone = "(11) 99999-0000",
                hospital = "Maternidade Santa Clara"),
            defaultBag().mapIndexed { i, it -> it.copy(done = i < 5) }, 60, "1.0.1", {}, {}, {}, {}, {}, {},
        )
    }
    @Test fun bag() = shot("10-bag") { BagScreen(defaultBag().mapIndexed { i, it -> it.copy(done = i < 5) }, {}, {}) }
}
