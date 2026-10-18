package com.walletid.app

import com.walletid.app.data.Qr
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QrTest {
    private fun pixels(text: String, size: Int, pad: Int): Triple<IntArray, Int, Int> {
        val m = Qr.encode(text, size)
        val w = m.width + pad * 2
        val px = IntArray(w * w) { -1 }
        for (y in 0 until m.height) for (x in 0 until m.width) {
            if (m.get(x, y)) px[(y + pad) * w + x + pad] = 0xFF000000.toInt()
        }
        return Triple(px, w, w)
    }

    @Test
    fun encodesAndReadsBack() {
        val text = "https://example.com/validate?doc=12345&nome=João"
        val (px, w, h) = pixels(text, 400, 60)
        val hit = Qr.decode(px, w, h)!!
        assertEquals(text, hit.text)
        // The reported box sits inside the image, around the code.
        assertTrue(hit.left >= 60f && hit.right <= w - 60f)
    }

    @Test
    fun readsLongPayloads() {
        val text = "A".repeat(900) + "1234567890".repeat(40)
        val (px, w, h) = pixels(text, 900, 20)
        assertEquals(text, Qr.decode(px, w, h)!!.text)
    }

    @Test
    fun noCodeInBlankImage() {
        assertNull(Qr.decode(IntArray(200 * 200) { -1 }, 200, 200))
    }
}
