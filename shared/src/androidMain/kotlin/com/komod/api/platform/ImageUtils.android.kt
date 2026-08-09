package com.komod.api.platform

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual fun ByteArray.toImageBitmap(): ImageBitmap {
    return BitmapFactory.decodeByteArray(this, 0, size).asImageBitmap()
}

actual suspend fun cropToSquareJpeg(
    bytes: ByteArray,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    outputSize: Int,
    quality: Int,
): ByteArray = withContext(Dispatchers.Default) {
    val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        ?: error("Unable to decode image for cropping.")

    val cropLeft = (x * original.width).roundToInt().coerceIn(0, original.width - 1)
    val cropTop = (y * original.height).roundToInt().coerceIn(0, original.height - 1)
    val cropWidth = (width * original.width).roundToInt().coerceIn(1, original.width - cropLeft)
    val cropHeight = (height * original.height).roundToInt().coerceIn(1, original.height - cropTop)
    val cropped = Bitmap.createBitmap(original, cropLeft, cropTop, cropWidth, cropHeight)

    val scale = outputSize.toFloat() / maxOf(cropped.width, cropped.height)
    val scaledWidth = (cropped.width * scale).roundToInt().coerceAtLeast(1)
    val scaledHeight = (cropped.height * scale).roundToInt().coerceAtLeast(1)
    val scaled = Bitmap.createScaledBitmap(cropped, scaledWidth, scaledHeight, true)

    val canvasBitmap = Bitmap.createBitmap(outputSize, outputSize, Bitmap.Config.ARGB_8888)
    Canvas(canvasBitmap).apply {
        drawColor(Color.WHITE)
        drawBitmap(scaled, (outputSize - scaledWidth) / 2f, (outputSize - scaledHeight) / 2f, null)
    }

    ByteArrayOutputStream().use { output ->
        canvasBitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
        output.toByteArray()
    }
}

actual suspend fun createThumbnail(bytes: ByteArray, maxDimension: Int): ByteArray = withContext(Dispatchers.Default) {
    val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        ?: error("Unable to decode image for thumbnail.")

    val scale = maxDimension.toFloat() / maxOf(original.width, original.height)
    val scaled = if (scale < 1f) {
        val targetWidth = (original.width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (original.height * scale).toInt().coerceAtLeast(1)
        Bitmap.createScaledBitmap(original, targetWidth, targetHeight, true)
    } else {
        original
    }

    ByteArrayOutputStream().use { output ->
        scaled.compress(Bitmap.CompressFormat.JPEG, 80, output)
        output.toByteArray()
    }
}
