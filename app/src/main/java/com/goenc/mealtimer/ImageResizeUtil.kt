package com.goenc.mealtimer

import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

private const val MaxImageLongSidePx = 1_024
private const val JpegQuality = 75

object ImageResizeUtil {
    fun saveResizedJpeg(
        bitmap: Bitmap,
        destination: File,
    ) {
        destination.parentFile?.mkdirs()

        val resized = resizeToLongSide(bitmap, MaxImageLongSidePx)
        FileOutputStream(destination).use { output ->
            resized.compress(Bitmap.CompressFormat.JPEG, JpegQuality, output)
        }
    }

    private fun resizeToLongSide(
        bitmap: Bitmap,
        maxLongSide: Int,
    ): Bitmap {
        val longSide = max(bitmap.width, bitmap.height)
        if (longSide <= maxLongSide) {
            return bitmap
        }

        val scale = maxLongSide.toFloat() / longSide.toFloat()
        val width = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
        val height = (bitmap.height * scale).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }
}
