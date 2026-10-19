package com.prenatal.app

import com.prenatal.app.data.Backup
import com.prenatal.app.data.Attachment
import com.prenatal.app.data.Event
import com.prenatal.app.data.EventKind
import com.prenatal.app.data.Gestation
import com.prenatal.app.data.Measurement
import com.prenatal.app.data.PregnancyData
import com.prenatal.app.data.Profile
import com.prenatal.app.data.Protocol
import com.prenatal.app.data.ProtocolStatus
import com.prenatal.app.data.Result
import com.prenatal.app.data.WrongPassphraseException
import com.prenatal.app.reminders.Reminders
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class PregnancyTest {
    private val today = LocalDate.of(2026, 10, 19)

    @Test
    fun gestationalAgeFromDueDate() {
        val g = Gestation.fromDueDate(today.plusDays(280 - (24 * 7 + 3)), today)
        assertEquals(24, g.weeks)
        assertEquals(3, g.dayOfWeek)
        assertEquals(2, g.trimester)
        assertEquals("24 weeks 3 days", g.label)
        assertEquals(280L - (24 * 7 + 3), g.daysToGo)
    }

    @Test
    fun naegelesRule() {
        assertEquals(LocalDate.of(2027, 1, 8), Gestation.dueDateFromLmp(LocalDate.of(2026, 4, 3)))
    }

    @Test
    fun trimesterBoundaries() {
        fun tri(weeks: Int) = Gestation.fromDueDate(today.plusDays(280L - weeks * 7), today).trimester
        assertEquals(1, tri(13))
        assertEquals(2, tri(14))
        assertEquals(2, tri(27))
        assertEquals(3, tri(28))
    }

    @Test
    fun checkupStatus() {
        val ogtt = Protocol.get("ogtt")!!
        assertEquals(ProtocolStatus.UPCOMING, Protocol.status(ogtt, 20, emptyList(), emptySet()))
        assertEquals(ProtocolStatus.DUE_NOW, Protocol.status(ogtt, 25, emptyList(), emptySet()))
        assertEquals(ProtocolStatus.OVERDUE, Protocol.status(ogtt, 30, emptyList(), emptySet()))
        val booked = Event(kind = EventKind.LAB, title = "TOTG", at = LocalDateTime.of(2026, 11, 2, 8, 0), protocolKey = "ogtt")
        assertEquals(ProtocolStatus.SCHEDULED, Protocol.status(ogtt, 25, listOf(booked), emptySet()))
        assertEquals(ProtocolStatus.DONE, Protocol.status(ogtt, 30, listOf(booked.copy(done = true)), emptySet()))
        assertEquals(ProtocolStatus.SKIPPED, Protocol.status(ogtt, 25, emptyList(), setOf("ogtt")))
    }

    @Test
    fun remindersDayBeforeAndTwoHoursBefore() {
        val now = LocalDateTime.of(2026, 10, 19, 12, 0)
        val e = Event(title = "Prenatal visit", at = LocalDateTime.of(2026, 10, 22, 14, 30), place = "Clinic")
        val plan = Reminders.plan(listOf(e, e.copy(id = "past", at = now.minusDays(1)), e.copy(id = "off", remind = false)), now)
        assertEquals(2, plan.size)
        assertEquals(LocalDateTime.of(2026, 10, 21, 9, 0), plan[0].at)
        assertEquals(LocalDateTime.of(2026, 10, 22, 12, 30), plan[1].at)
        assertTrue(plan[0].title.contains("Prenatal visit"))
    }

    @Test
    fun recordSurvivesJsonAndEncryptedBackup() {
        val att = Attachment(name = "hemograma.pdf", mime = "application/pdf")
        val data = PregnancyData(
            profile = Profile(motherName = "Ana", dueDate = LocalDate.of(2027, 3, 1), bloodType = "O+"),
            events = listOf(Event(title = "Morfológico", kind = EventKind.ULTRASOUND, at = LocalDateTime.of(2026, 11, 10, 10, 0), protocolKey = "us-morph")),
            results = listOf(Result(title = "Hemograma", date = today, values = "Hemoglobin: 12.1 g/dL", attachments = listOf(att))),
            measurements = listOf(Measurement(date = today, weightKg = 64.5, bpSystolic = 110, bpDiastolic = 70)),
            skippedProtocol = setOf("anti-d"),
        )
        assertEquals(data, PregnancyData.fromJson(data.toJson()))

        val pdf = ByteArray(3000) { (it % 251).toByte() }
        val text = Backup.export(data, { if (it == att.id) pdf else null }, "a good passphrase", iterations = 1000)
        val (back, files) = Backup.parse(text, "a good passphrase")
        assertEquals(data, back)
        assertArrayEquals(pdf, files.getValue(att.id))
    }

    @Test(expected = WrongPassphraseException::class)
    fun wrongPassphrase() {
        val text = Backup.export(PregnancyData(), { null }, "right one!", iterations = 1000)
        Backup.parse(text, "wrong one!")
    }
}
