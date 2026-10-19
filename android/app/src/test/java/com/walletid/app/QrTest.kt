package com.walletid.app

import com.walletid.app.data.Qr
import com.walletid.app.data.QrModules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QrTest {
    @Test
    fun readsTheExactGridBack() {
        val code = Qr.encode("https://example.com/validate?doc=12345&nome=João")
        val (px, w, h) = Qr.pixels(code, scale = 6)
        val read = Qr.read(px, w, h)
        assertNotNull(read)
        assertEquals(code.text, read!!.text)
        assertTrue("same modules, square for square", read.sameGridAs(code))
    }

    @Test
    fun denseCodesLikeTheCnh() {
        // The CNH's code carries ~485 characters (a 77×77 grid).
        val text = (0 until 485).map { "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789+/="[(it * 7) % 39] }.joinToString("")
        val code = Qr.encode(text)
        assertTrue(code.size >= 69)
        val (px, w, h) = Qr.pixels(code, scale = 4)
        val read = Qr.read(px, w, h)!!
        assertEquals(text, read.text)
        assertTrue(read.sameGridAs(code))
    }

    @Test
    fun storageRoundTrip() {
        val code = Qr.encode("SAMPLE 000001")
        val back = QrModules.parse(code.text, code.serialize())!!
        assertTrue(back.sameGridAs(code))
        assertEquals("SAMPLE 000001", back.text)
        assertNull(QrModules.parse("x", "garbage"))
    }

    @Test
    fun noCodeInBlankImage() {
        assertNull(Qr.read(IntArray(200 * 200) { -1 }, 200, 200))
    }
}
