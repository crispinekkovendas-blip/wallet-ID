package com.walletid.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.walletid.app.ui.QrCodeView
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.walletid.app.data.Doc
import com.walletid.app.data.Images
import com.walletid.app.data.Qr
import com.walletid.app.data.Side
import com.walletid.app.ui.CardScreen
import com.walletid.app.ui.EditScreen
import com.walletid.app.ui.LockScreen
import com.walletid.app.ui.SettingsScreen
import com.walletid.app.ui.WalletScreen
import com.walletid.app.ui.WalletTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Renders every screen with fake sample cards so the design can be reviewed without a phone. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h852dp-xhdpi")
class ScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private val dir = File(System.getProperty("screenshots.dir") ?: "build/screenshots").apply { mkdirs() }
    private fun shot(name: String, content: @Composable () -> Unit) {
        compose.setContent { WalletTheme(content) }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage(File(dir, "$name.png").absolutePath)
    }

    private val docs = listOf(
        Doc("rg", mapOf("type" to "rg", "title" to "RG")),
        Doc("cnh", mapOf("type" to "cnh", "title" to "CNH", "qr" to SAMPLE_QR)),
        Doc("creci", mapOf("type" to "creci", "title" to "CRECI")),
        Doc("sus", mapOf("type" to "sus", "title" to "Cartão SUS")),
    )

    /** A fake ID card photo: coloured card, header, photo box and text lines. */
    private fun fakeCard(c1: Int, c2: Int, header: String, sub: String, withQr: Boolean = false): Bitmap {
        val w = 1000
        val h = 630
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawPaint(Paint().apply { shader = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), c1, c2, Shader.TileMode.CLAMP) })
        val white = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = -1 }
        val faint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x66FFFFFF }
        c.drawText(header, 48f, 92f, Paint(white).apply { textSize = 56f; typeface = Typeface.DEFAULT_BOLD })
        c.drawText(sub, 48f, 136f, Paint(white).apply { textSize = 26f; alpha = 210 })
        c.drawText("REPÚBLICA FEDERATIVA DO BRASIL", 560f, 70f, Paint(white).apply { textSize = 20f; alpha = 200 })
        if (withQr) {
            val qr = Qr.toBitmap(Qr.encode(SAMPLE_QR), scale = 8)
            c.drawRoundRect(RectF(620f, 220f, 960f, 560f), 12f, 12f, white)
            c.drawBitmap(qr, null, RectF(625f, 225f, 955f, 555f), null)
        } else {
            c.drawRoundRect(RectF(48f, 190f, 288f, 500f), 16f, 16f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x55FFFFFF })
            c.drawCircle(168f, 300f, 60f, faint)
            c.drawRoundRect(RectF(88f, 380f, 248f, 500f), 60f, 60f, faint)
        }
        val x = if (withQr) 48f else 330f
        for (i in 0 until 5) {
            c.drawRoundRect(RectF(x, 200f + i * 70f, x + 260f, 216f + i * 70f), 8f, 8f, faint)
            c.drawRoundRect(RectF(x, 226f + i * 70f, x + 200f + (i % 3) * 40f, 250f + i * 70f), 8f, 8f, white)
        }
        return bmp
    }

    private val green = fakeCard(0xFF1D5A2C.toInt(), 0xFF7FB238.toInt(), "CNH", "Carteira Nacional de Habilitação")
    private val greenBack = fakeCard(0xFF2D6B3A.toInt(), 0xFF9CC25A.toInt(), "VERSO", "Observações · QR Code", withQr = true)
    private val thumbs: Map<String, ImageBitmap?> = mapOf(
        "rg" to fakeCard(0xFF0F5C4A.toInt(), 0xFF2A9D7C.toInt(), "RG", "Carteira de Identidade").asImageBitmap(),
        "cnh" to green.asImageBitmap(),
        "creci" to fakeCard(0xFF151A2E.toInt(), 0xFF3A3F63.toInt(), "CRECI", "Conselho Regional de Corretores de Imóveis").asImageBitmap(),
        "sus" to fakeCard(0xFF08506B.toInt(), 0xFF1A97B8.toInt(), "SUS", "Cartão Nacional de Saúde").asImageBitmap(),
    )

    @Test fun lock() = shot("01-lock") { LockScreen(true, null, {}, {}) }

    @Test fun walletEmpty() = shot("02-wallet-empty") { WalletScreen(emptyList(), emptyMap(), 0, "", {}, {}, {}, {}, {}) }

    @Test fun wallet() = shot("03-wallet") { WalletScreen(docs, thumbs, docs.size, "", {}, {}, {}, {}, {}) }

    private fun card(name: String, flipped: Boolean, showQr: Boolean) = shot(name) {
        CardScreen(
            doc = docs[1], front = green.asImageBitmap(), back = greenBack.asImageBitmap(),
            hasQr = true, qr = Qr.encode(DENSE_QR), qrPicture = null, showQr = showQr,
            flipped = flipped, onFlip = {}, onShowQr = {}, onBack = {}, onEdit = {}, onShare = {}, onDelete = {},
        )
    }

    @Test fun cardFront() = card("04-card-front", flipped = false, showQr = false)

    @Test fun cardBack() = card("05-card-back", flipped = true, showQr = false)

    @Test fun cardQr() = card("06-card-qr", flipped = false, showQr = true)

    @Test fun addEmpty() = shot("07-add-card") {
        EditScreen(EditSession(null, "other", emptyMap()), {}, {}, {}, {}, {}, {}, { _, _ -> }, {}, {})
    }

    @Test fun addFilled() {
        val code = Qr.encode(SAMPLE_QR)
        val session = EditSession(null, "cnh", mapOf("title" to "CNH", "qr" to code.text, "qrModules" to code.serialize()))
        session.previews[Side.FRONT] = green.asImageBitmap()
        session.previews[Side.BACK] = greenBack.asImageBitmap()
        shot("08-add-card-filled") { EditScreen(session, {}, {}, {}, {}, {}, {}, { _, _ -> }, {}, {}) }
    }

    @Test fun settings() = shot("09-settings") { SettingsScreen(0, "1.0.3", {}, {}, {}, {}, {}) }

    @Test fun findsQrPrintedOnTheBackPhoto() {
        val found = Qr.find(greenBack)
        assertNotNull("QR code on the fake verso should be detected", found)
        assertEquals(SAMPLE_QR, found!!.text)
        assertTrue(found.sameGridAs(Qr.encode(SAMPLE_QR)))
    }

    @Test fun qrViewScansBackExactly() {
        // What the app draws (rounded corner markers included) must scan to the very same grid.
        val code = Qr.encode(DENSE_QR)
        compose.setContent { QrCodeView(code, Modifier.size(260.dp)) }
        compose.waitForIdle()
        // Robolectric can't use Compose's captureToImage (it times out); capture with Roborazzi and read the PNG back.
        val file = File(dir, "qr-scan-check.png")
        compose.onRoot().captureRoboImage(file.absolutePath)
        val img = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
        file.delete()
        val px = IntArray(img.width * img.height)
        img.getPixels(px, 0, img.width, 0, 0, img.width, img.height)
        val read = Qr.read(px, img.width, img.height)
        assertNotNull("rendered code should scan", read)
        assertEquals(DENSE_QR, read!!.text)
        assertTrue("rendered code keeps the exact grid", read.sameGridAs(code))
    }

    @Test fun portraitScreenshotBecomesTrimmedLandscapeCard() {
        // A phone screenshot of a sideways card: grey margins around a 1000×630 card, rotated to portrait.
        val card = fakeCard(0xFF151A2E.toInt(), 0xFF3A3F63.toInt(), "CRECI", "Identidade profissional")
        val shot = Bitmap.createBitmap(630 + 60, 1000 + 80, Bitmap.Config.ARGB_8888)
        Canvas(shot).apply {
            drawColor(0xFFEDEDED.toInt())
            rotate(90f)
            drawBitmap(card, 40f, -(630f + 30f), null)
        }
        val out = Images.normalize(shot)
        assertTrue("landscape", out.width > out.height)
        assertEquals(1000f, out.width.toFloat(), 6f)
        assertEquals(630f, out.height.toFloat(), 6f)
    }

    companion object {
        const val SAMPLE_QR = "https://example.com/validate?doc=CNH-SAMPLE-0001"
        // Fake payload as long as a real CNH code, to preview how a dense code renders.
        val DENSE_QR = (0 until 485).map { "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"[(it * 11) % 36] }.joinToString("")
    }
}
