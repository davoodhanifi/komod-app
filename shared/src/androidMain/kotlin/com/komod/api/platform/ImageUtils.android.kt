package com.komod.api.platform

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual fun ByteArray.toImageBitmap(): ImageBitmap {
    return BitmapFactory.decodeByteArray(this, 0, size).asImageBitmap()
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
