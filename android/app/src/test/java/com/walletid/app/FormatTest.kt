package com.walletid.app

import com.walletid.app.data.Doc
import com.walletid.app.data.Expiry
import com.walletid.app.data.ExpiryLevel
import com.walletid.app.data.formatBrDate
import com.walletid.app.data.formatCpf
import com.walletid.app.data.mask
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FormatTest {
    @Test
    fun formatsCpfWhileTyping() {
        assertEquals("123", formatCpf("123"))
        assertEquals("123.4", formatCpf("1234"))
        assertEquals("123.456.7", formatCpf("1234567"))
        assertEquals("123.456.789-01", formatCpf("12345678901"))
        assertEquals("123.456.789-01", formatCpf("123.456.789-0199"))
    }

    @Test
    fun masksAllButLastThree() {
        assertEquals("•••.•••.••9-01", mask("123.456.789-01"))
        assertEquals("•••••456", mask("FX123456"))
        assertEquals("12", mask("12"))
    }

    @Test
    fun expiryLevels() {
        val today = LocalDate.of(2026, 10, 18)
        assertEquals(ExpiryLevel.BAD, Expiry.of("2026-10-17", today)!!.level)
        assertEquals("Expires today", Expiry.of("2026-10-18", today)!!.text)
        assertEquals(ExpiryLevel.WARN, Expiry.of("2026-12-20", today)!!.level)
        assertNull(Expiry.of("2027-06-01", today))
        assertNull(Expiry.of("", today))
    }

    @Test
    fun brazilianDates() {
        assertEquals("20/12/2026", formatBrDate("2026-12-20"))
    }

    @Test
    fun searchAndTitles() {
        val d = Doc("1", mapOf("type" to "council", "council" to "OAB", "number" to "123456"))
        assertEquals("OAB", d.title)
        assertTrue(d.matches("oab"))
        assertTrue(d.matches("conselho"))
        assertFalse(d.matches("cnh"))
        assertEquals("Document", Doc("2", mapOf("type" to "other")).title)
    }
}
