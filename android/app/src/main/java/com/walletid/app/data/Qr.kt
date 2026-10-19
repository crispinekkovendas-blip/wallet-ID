package com.walletid.app.data

import android.graphics.Bitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.decoder.Decoder
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.detector.Detector
import com.google.zxing.qrcode.encoder.Encoder
import java.util.Base64
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * A QR code as its grid of modules (the black/white squares) plus the text it carries.
 * Storing the grid instead of a picture lets the app redraw the exact same code razor-sharp
 * at any size, so official codes stay valid while looking high-definition.
 */
class QrModules(val text: String, val size: Int, private val bits: BooleanArray) {
    operator fun get(x: Int, y: Int): Boolean = bits[y * size + x]

    /** Compact form for storage: "1:<size>:<base64 of the packed bits>". */
    fun serialize(): String {
        val packed = ByteArray((bits.size + 7) / 8)
        bits.forEachIndexed { i, on -> if (on) packed[i / 8] = (packed[i / 8].toInt() or (0x80 ushr (i % 8))).toByte() }
        return "1:$size:" + Base64.getEncoder().encodeToString(packed)
    }

    fun sameGridAs(other: QrModules) = size == other.size && bits.contentEquals(other.bits)

    companion object {
        fun parse(text: String, stored: String): QrModules? = runCatching {
            val (version, size, data) = stored.split(":", limit = 3)
            require(version == "1")
            val n = size.toInt()
            val packed = Base64.getDecoder().decode(data)
            QrModules(text, n, BooleanArray(n * n) { i -> packed[i / 8].toInt() and (0x80 ushr (i % 8)) != 0 })
        }.getOrNull()

        fun of(text: String, matrix: BitMatrix) =
            QrModules(text, matrix.width, BooleanArray(matrix.width * matrix.height) { i -> matrix.get(i % matrix.width, i / matrix.width) })
    }
}

object Qr {
    private val hints = mapOf(DecodeHintType.TRY_HARDER to true)

    /**
     * Finds a QR code in ARGB pixels and returns its exact module grid. The grid is checked by
     * decoding it, so what we keep is precisely what the original code encodes.
     */
    fun read(pixels: IntArray, width: Int, height: Int): QrModules? {
        val source = RGBLuminanceSource(width, height, pixels)
        for (binarizer in listOf(HybridBinarizer(source), GlobalHistogramBinarizer(source))) {
            try {
                val detected = Detector(binarizer.blackMatrix).detect(hints)
                // Copy first: the decoder unmasks the grid in place while reading it.
                val grid = detected.bits.clone()
                val decoded = Decoder().decode(detected.bits, hints)
                return QrModules.of(decoded.text, grid)
            } catch (e: ReaderException) {
                // try the next binarizer
            }
        }
        return null
    }

    /** Looks for a QR code in a photo, trying a few sizes (small codes in big photos read better downscaled). */
    fun find(photo: Bitmap): QrModules? {
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
            if (scaled != photo) scaled.recycle()
            read(px, scaled.width, scaled.height)?.let { return it }
        }
        return null
    }

    /** A new code for text the user typed. */
    fun encode(text: String): QrModules {
        val m = Encoder.encode(text, ErrorCorrectionLevel.M, mapOf(EncodeHintType.CHARACTER_SET to "UTF-8")).matrix
        return QrModules(text, m.width, BooleanArray(m.width * m.height) { i -> m.get(i % m.width, i / m.width).toInt() == 1 })
    }

    /** Draws the code into pixels: [scale] pixels per module, [quiet] modules of white border. */
    fun pixels(code: QrModules, scale: Int, quiet: Int = 4): Triple<IntArray, Int, Int> {
        val n = (code.size + quiet * 2) * scale
        val px = IntArray(n * n) { WHITE }
        for (y in 0 until code.size) for (x in 0 until code.size) {
            if (!code[x, y]) continue
            for (dy in 0 until scale) {
                val row = ((y + quiet) * scale + dy) * n
                for (dx in 0 until scale) px[row + (x + quiet) * scale + dx] = BLACK
            }
        }
        return Triple(px, n, n)
    }

    fun toBitmap(code: QrModules, scale: Int = 12): Bitmap {
        val (px, w, h) = pixels(code, scale)
        return Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888)
    }

    private const val BLACK = 0xFF000000.toInt()
    private const val WHITE = 0xFFFFFFFF.toInt()
}
