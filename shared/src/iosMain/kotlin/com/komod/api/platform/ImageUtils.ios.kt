package com.komod.api.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface

actual fun ByteArray.toImageBitmap(): ImageBitmap {
    return Image.makeFromEncoded(this).toComposeImageBitmap()
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
