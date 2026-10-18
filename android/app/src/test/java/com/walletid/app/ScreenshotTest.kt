package com.walletid.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.walletid.app.data.Doc
import com.walletid.app.data.Side
import com.walletid.app.ui.DetailScreen
import com.walletid.app.ui.EditScreen
import com.walletid.app.ui.LockScreen
import com.walletid.app.ui.PickerScreen
import com.walletid.app.ui.SettingsScreen
import com.walletid.app.ui.WalletScreen
import com.walletid.app.ui.WalletTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate

/** Renders every screen with sample (fake) data so the design can be reviewed without a phone. */
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

    private val today = LocalDate.now()
    private val docs = listOf(
        Doc("rg", mapOf("type" to "rg", "holder" to "Maria Silva Santos", "number" to "12.345.678-9", "issuer" to "SSP", "uf" to "SP",
            "cpf" to "123.456.789-01", "birth" to "1990-04-12", "issued" to "2018-03-02")),
        Doc("cnh", mapOf("type" to "cnh", "holder" to "Maria Silva Santos", "number" to "01234567890", "category" to "AB",
            "cpf" to "123.456.789-01", "expires" to today.plusDays(63).toString(), "issuer" to "DETRAN-SP")),
        Doc("creci", mapOf("type" to "creci", "holder" to "Maria Silva Santos", "number" to "123456-F", "region" to "CRECI-SP · 2ª Região",
            "kind" to "Pessoa Física", "expires" to today.plusYears(1).toString())),
        Doc("pass", mapOf("type" to "passport", "holder" to "Maria Silva Santos", "number" to "FX123456", "country" to "Brasil",
            "expires" to today.plusYears(5).toString())),
    )

    private fun samplePhoto() = Bitmap.createBitmap(856, 540, Bitmap.Config.ARGB_8888).apply {
        val c = Canvas(this)
        c.drawPaint(Paint().apply { shader = LinearGradient(0f, 0f, 856f, 540f, 0xFF1D5A2C.toInt(), 0xFF7FB238.toInt(), Shader.TileMode.CLAMP) })
        c.drawText("SAMPLE PHOTO", 40f, 90f, Paint().apply { color = -1; textSize = 48f })
    }.asImageBitmap()

    @Test fun lock() = shot("01-lock") { LockScreen(true, null, {}, {}) }

    @Test fun walletEmpty() = shot("02-wallet-empty") { WalletScreen(emptyList(), 0, "", {}, {}, {}, {}, {}) }

    @Test fun wallet() = shot("03-wallet") { WalletScreen(docs, docs.size, "", {}, {}, {}, {}, {}) }

    @Test fun picker() = shot("04-picker") { PickerScreen({}, {}) }

    @Test fun detail() = shot("05-detail-cnh") {
        DetailScreen(docs[1], false, samplePhoto(), null, {}, {}, {}, { _, _ -> }, {}, {}, {})
    }

    @Test fun detailRevealed() = shot("06-detail-rg-revealed") {
        DetailScreen(docs[0], true, null, null, {}, {}, {}, { _, _ -> }, {}, {}, {})
    }

    @Test fun edit() {
        val session = EditSession(null, "creci", mapOf("holder" to "Maria Silva Santos", "number" to "123456-F", "region" to "CRECI-SP · 2ª Região"))
        session.previews[Side.FRONT] = samplePhoto()
        shot("07-edit-creci") { EditScreen(session, {}, {}, {}, {}, {}) }
    }

    @Test fun settings() = shot("08-settings") { SettingsScreen(0, "1.0.1", {}, {}, {}, {}, {}) }
}
