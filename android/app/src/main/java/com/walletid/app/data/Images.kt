package com.walletid.app.data

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object Images {
    private const val MAX_SIDE = 2400

    /**
     * Reads a picked photo or screenshot and makes it a clean, horizontal card image:
     * fixes camera rotation, turns portrait screenshots of a card to landscape,
     * and trims the plain screenshot margins around the card.
     */
    fun prepare(resolver: ContentResolver, uri: Uri): ByteArray {
        val original = resolver.openInputStream(uri)?.use { it.readBytes() } ?: error("Couldn’t open that image")
        val rotation = runCatching { ExifInterface(original.inputStream()).rotationDegrees }.getOrDefault(0)
        var bmp = decode(original, MAX_SIDE) ?: error("That file isn’t an image")
        if (rotation != 0) bmp = rotated(bmp, rotation)
        return encode(normalize(bmp))
    }

    /** Portrait → landscape (counter-clockwise, how phones show a card sideways), then trim margins. */
    fun normalize(bmp: Bitmap): Bitmap {
        val landscape = if (bmp.height > bmp.width) rotated(bmp, -90) else bmp
        return trimMargins(landscape)
    }

    /** Rotates stored image bytes by [degrees] (the edit screen's Rotate button). */
    fun rotate(bytes: ByteArray, degrees: Int): ByteArray {
        val bmp = decode(bytes, MAX_SIDE) ?: error("Couldn’t read that image")
        return encode(rotated(bmp, degrees))
    }

    fun isPortrait(bytes: ByteArray): Boolean {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, o)
        return o.outHeight > o.outWidth
    }

    fun encode(bmp: Bitmap): ByteArray = ByteArrayOutputStream().use {
        bmp.compress(Bitmap.CompressFormat.JPEG, 95, it)
        it.toByteArray()
    }

    private fun rotated(bmp: Bitmap, degrees: Int): Bitmap {
        val out = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(degrees.toFloat()) }, true)
        if (out != bmp) bmp.recycle()
        return out
    }

    /**
     * Removes plain borders around the card: lines that are a single neutral grey/white,
     * allowing a soft shadow gradient but stopping at the first sharp change (the card's edge).
     */
    fun trimMargins(bmp: Bitmap): Bitmap {
        val w = bmp.width
        val h = bmp.height
        if (w < 50 || h < 50) return bmp
        val px = IntArray(w * h)
        bmp.getPixels(px, 0, w, 0, 0, w, h)
        val step = max(1, min(w, h) / 400)

        fun scan(limit: Int, line: (Int) -> IntArray): Int {
            var prev: Int? = null
            var n = 0
            while (n < limit) {
                val color = marginColor(line(n)) ?: break
                if (prev != null && distance(color, prev) > 24) break
                prev = color
                n++
            }
            return n
        }

        fun row(y: Int) = IntArray((w + step - 1) / step) { px[y * w + it * step] }
        val top = scan(h * 15 / 100) { row(it) }
        val bottom = scan(h * 15 / 100) { row(h - 1 - it) }
        val rows = h - top - bottom
        fun col(x: Int) = IntArray((rows + step - 1) / step) { px[(top + it * step) * w + x] }
        val left = scan(w * 15 / 100) { col(it) }
        val right = scan(w * 15 / 100) { col(w - 1 - it) }

        val cw = w - left - right
        val ch = h - top - bottom
        if (cw == w && ch == h) return bmp
        if (cw < w / 2 || ch < h / 2) return bmp // something went wrong; keep the photo as it is
        val out = Bitmap.createBitmap(bmp, left, top, cw, ch)
        if (out != bmp) bmp.recycle()
        return out
    }

    /** The line's typical colour if the line is flat and neutral (a margin), else null. */
    private fun marginColor(line: IntArray): Int? {
        val sorted = line.sortedBy { sum(it) }
        val median = sorted[sorted.size / 2]
        val r = median shr 16 and 0xFF
        val g = median shr 8 and 0xFF
        val b = median and 0xFF
        if (max(r, max(g, b)) - min(r, min(g, b)) > 6) return null
        val flat = line.count { distance(it, median) <= 30 }
        return if (flat >= line.size * 97 / 100) median else null
    }

    private fun sum(c: Int) = (c shr 16 and 0xFF) + (c shr 8 and 0xFF) + (c and 0xFF)

    private fun distance(a: Int, b: Int) =
        abs((a shr 16 and 0xFF) - (b shr 16 and 0xFF)) + abs((a shr 8 and 0xFF) - (b shr 8 and 0xFF)) + abs((a and 0xFF) - (b and 0xFF))

    /** Decodes, downsampled so the longest side is at most [maxSide] pixels. */
    fun decode(bytes: ByteArray, maxSide: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val longest = max(bounds.outWidth, bounds.outHeight)
        if (longest <= 0) return null
        var sample = 1
        while (longest / (sample * 2) >= maxSide) sample *= 2
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: return null
        val scale = maxSide.toFloat() / max(bmp.width, bmp.height)
        if (scale >= 1f) return bmp
        return Bitmap.createScaledBitmap(bmp, (bmp.width * scale).roundToInt(), (bmp.height * scale).roundToInt(), true)
            .also { if (it != bmp) bmp.recycle() }
    }
}
