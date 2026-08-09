package com.komod.api.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlin.math.roundToInt
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface

actual fun ByteArray.toImageBitmap(): ImageBitmap {
    return Image.makeFromEncoded(this).toComposeImageBitmap()
}

actual suspend fun cropToSquareJpeg(
    bytes: ByteArray,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    outputSize: Int,
    quality: Int,
): ByteArray {
    val original = Image.makeFromEncoded(bytes)

    val cropLeft = (x * original.width).roundToInt().coerceIn(0, original.width - 1)
    val cropTop = (y * original.height).roundToInt().coerceIn(0, original.height - 1)
    val cropWidth = (width * original.width).roundToInt().coerceIn(1, original.width - cropLeft)
    val cropHeight = (height * original.height).roundToInt().coerceIn(1, original.height - cropTop)

    val scale = outputSize.toFloat() / maxOf(cropWidth, cropHeight)
    val scaledWidth = (cropWidth * scale).roundToInt().coerceAtLeast(1)
    val scaledHeight = (cropHeight * scale).roundToInt().coerceAtLeast(1)
    val left = (outputSize - scaledWidth) / 2f
    val top = (outputSize - scaledHeight) / 2f

    val surface = Surface.makeRasterN32Premul(outputSize, outputSize)
    surface.canvas.apply {
        clear(0xFFFFFFFF.toInt())
        drawImageRect(
            original,
            Rect.makeXYWH(cropLeft.toFloat(), cropTop.toFloat(), cropWidth.toFloat(), cropHeight.toFloat()),
            Rect.makeXYWH(left, top, scaledWidth.toFloat(), scaledHeight.toFloat()),
        )
    }

    return surface.makeImageSnapshot().encodeToData(EncodedImageFormat.JPEG, quality)?.bytes
        ?: error("Unable to encode cropped image.")
}

actual suspend fun createThumbnail(bytes: ByteArray, maxDimension: Int): ByteArray {
    val original = Image.makeFromEncoded(bytes)
    val scale = maxDimension.toFloat() / maxOf(original.width, original.height)

    val resized = if (scale < 1f) {
        val targetWidth = (original.width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (original.height * scale).toInt().coerceAtLeast(1)
        val surface = Surface.makeRasterN32Premul(targetWidth, targetHeight)
        surface.canvas.drawImageRect(original, Rect.makeWH(targetWidth.toFloat(), targetHeight.toFloat()))
        surface.makeImageSnapshot()
    } else {
        original
    }

    return resized.encodeToData(EncodedImageFormat.JPEG, 80)?.bytes
        ?: error("Unable to encode thumbnail.")
}
