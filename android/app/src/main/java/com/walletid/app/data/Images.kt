package com.walletid.app.data

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

object Images {
    private const val MAX_SIDE = 2400
    private const val KEEP_ORIGINAL_BYTES = 4 * 1024 * 1024

    /**
     * Reads a picked photo or screenshot. Screenshots are kept byte-for-byte;
     * big or rotated camera photos are resized and saved as JPEG.
     */
    fun prepare(resolver: ContentResolver, uri: Uri): ByteArray {
        val original = resolver.openInputStream(uri)?.use { it.readBytes() } ?: error("Couldn’t open that image")
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(original, 0, original.size, bounds)
        if (bounds.outWidth <= 0) error("That file isn’t an image")

        val rotation = runCatching {
            ExifInterface(original.inputStream()).rotationDegrees
        }.getOrDefault(0)
        val mime = Backup.sniffMime(original)
        val longest = max(bounds.outWidth, bounds.outHeight)
        if (rotation == 0 && longest <= MAX_SIDE && original.size <= KEEP_ORIGINAL_BYTES && mime != "image/webp") {
            return original
        }

        var sample = 1
        while (longest / (sample * 2) >= MAX_SIDE) sample *= 2
        var bmp = BitmapFactory.decodeByteArray(original, 0, original.size, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: error("Couldn’t read that image")
        val scale = MAX_SIDE.toFloat() / max(bmp.width, bmp.height)
        if (scale < 1f || rotation != 0) {
            val m = Matrix().apply {
                if (scale < 1f) postScale(scale, scale)
                if (rotation != 0) postRotate(rotation.toFloat())
            }
            val transformed = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
            if (transformed != bmp) bmp.recycle()
            bmp = transformed
        }
        return ByteArrayOutputStream().use { out ->
            bmp.compress(Bitmap.CompressFormat.JPEG, 92, out)
            bmp.recycle()
            out.toByteArray()
        }
    }

    /** Decodes for display, downsampled so the longest side is at most [maxSide] pixels. */
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
