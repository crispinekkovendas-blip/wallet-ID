package com.prenatal.app.ocr

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max

/**
 * On-device text recognition (ML Kit, model bundled in the app). The photo never leaves the phone.
 */
object TextScanner {
    private const val MAX_SIDE = 2400

    /** Reads the photo and returns its text as rows, in reading order. */
    suspend fun readRows(bytes: ByteArray): List<String> {
        val bmp = decodeUpright(bytes) ?: error("Couldn’t read that image")
        val text = suspendCancellableCoroutine { cont ->
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(InputImage.fromBitmap(bmp, 0))
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resumeWithException(it) }
                .addOnCompleteListener { recognizer.close() }
        }
        val lines = text.textBlocks.flatMap { block ->
            block.lines.mapNotNull { line ->
                val b = line.boundingBox ?: return@mapNotNull null
                OcrLine(line.text, b.left, b.top, b.right, b.bottom)
            }
        }
        return OcrParser.rows(lines)
    }

    /** Decodes, applies the camera's rotation (EXIF) and keeps the size reasonable for recognition. */
    private fun decodeUpright(bytes: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val longest = max(bounds.outWidth, bounds.outHeight)
        if (longest <= 0) return null
        var sample = 1
        while (longest / (sample * 2) >= MAX_SIDE) sample *= 2
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
        val rotation = runCatching { ExifInterface(bytes.inputStream()).rotationDegrees }.getOrDefault(0)
        if (rotation == 0) return bmp
        return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(rotation.toFloat()) }, true)
    }
}
