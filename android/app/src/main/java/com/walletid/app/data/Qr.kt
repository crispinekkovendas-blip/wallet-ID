package com.walletid.app.data

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.LuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** A QR code read from pixels: its text and where it sits (x0, y0, x1, y1) in those pixels. */
data class QrHit(val text: String, val left: Float, val top: Float, val right: Float, val bottom: Float)

/** A QR code found in a photo: its text plus a crop of the original pixels (PNG). */
class QrFound(val text: String, val cropPng: ByteArray)

object Qr {
    private val decodeHints = mapOf(
        DecodeHintType.TRY_HARDER to true,
        DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
    )

    /** Pure-JVM decode of ARGB pixels. */
    fun decode(pixels: IntArray, width: Int, height: Int): QrHit? {
        val source: LuminanceSource = RGBLuminanceSource(width, height, pixels)
        for (binary in listOf(BinaryBitmap(HybridBinarizer(source)), BinaryBitmap(GlobalHistogramBinarizer(source)))) {
            val result = try {
                QRCodeReader().decode(binary, decodeHints)
            } catch (e: ReaderException) {
                null
            } ?: continue
            val pts = result.resultPoints.filterNotNull()
            if (pts.isEmpty()) return QrHit(result.text, 0f, 0f, width.toFloat(), height.toFloat())
            return QrHit(result.text, pts.minOf { it.x }, pts.minOf { it.y }, pts.maxOf { it.x }, pts.maxOf { it.y })
        }
        return null
    }

    fun encode(text: String, size: Int = 720): BitMatrix =
        QRCodeWriter().encode(
            text,
            BarcodeFormat.QR_CODE,
            size,
            size,
            mapOf(
                EncodeHintType.MARGIN to 1,
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.CHARACTER_SET to "UTF-8",
            ),
        )

    fun toBitmap(m: BitMatrix): Bitmap {
        val pixels = IntArray(m.width * m.height) { i -> if (m.get(i % m.width, i / m.width)) BLACK else WHITE }
        return Bitmap.createBitmap(pixels, m.width, m.height, Bitmap.Config.ARGB_8888)
    }

    /**
     * Looks for a QR code in a photo, trying a few sizes (small QR codes in big photos
     * read better downscaled). Returns the text and a crop of the original pixels so the
     * code can be shown exactly as issued.
     */
    fun find(photo: Bitmap): QrFound? {
        val longest = max(photo.width, photo.height)
        val scales = listOf(1600, 1100, 800, 2400).map { min(1f, it.toFloat() / longest) }.distinct()
        for (scale in scales) {
            val scaled = if (scale < 1f) {
                Bitmap.createScaledBitmap(photo, (photo.width * scale).roundToInt(), (photo.height * scale).roundToInt(), true)
            } else {
                photo
            }
            val px = IntArray(scaled.width * scaled.height)
            scaled.getPixels(px, 0, scaled.width, 0, 0, scaled.width, scaled.height)
            val hit = decode(px, scaled.width, scaled.height)
            if (scaled != photo) scaled.recycle()
            if (hit != null) return QrFound(hit.text, crop(photo, hit, scale))
        }
        return null
    }

    /** Crops around the finder patterns, with enough margin to include the whole code and quiet zone. */
    private fun crop(photo: Bitmap, hit: QrHit, scale: Float): ByteArray {
        val l = hit.left / scale
        val t = hit.top / scale
        val r = hit.right / scale
        val b = hit.bottom / scale
        val size = max(r - l, b - t)
        // Finder centres sit 3.5 modules in from the edge; 30% covers that plus the quiet zone even for small codes.
        val pad = size * 0.3f
        val x0 = (l - pad).roundToInt().coerceIn(0, photo.width - 1)
        val y0 = (t - pad).roundToInt().coerceIn(0, photo.height - 1)
        val x1 = (r + pad).roundToInt().coerceIn(x0 + 1, photo.width)
        val y1 = (b + pad).roundToInt().coerceIn(y0 + 1, photo.height)
        val cropped = Bitmap.createBitmap(photo, x0, y0, x1 - x0, y1 - y0)
        return ByteArrayOutputStream().use {
            cropped.compress(Bitmap.CompressFormat.PNG, 100, it)
            if (cropped != photo) cropped.recycle()
            it.toByteArray()
        }
    }

    private const val BLACK = 0xFF000000.toInt()
    private const val WHITE = 0xFFFFFFFF.toInt()
}
