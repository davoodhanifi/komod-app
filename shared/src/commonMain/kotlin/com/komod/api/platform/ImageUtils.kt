package com.komod.api.platform

import androidx.compose.ui.graphics.ImageBitmap

expect fun ByteArray.toImageBitmap(): ImageBitmap

expect suspend fun createThumbnail(bytes: ByteArray, maxDimension: Int = 512): ByteArray

/**
 * Crops the normalized region (x, y, width, height, each 0f..1f) out of [bytes] — expected
 * to be the full-resolution original image, never a thumbnail or preview — and fits it into
 * an [outputSize] x [outputSize] square JPEG without stretching. The crop is scaled down to
 * fit entirely within the square (preserving its aspect ratio) and centered on a white
 * background; the backend replaces that background when it removes and recomposes the
 * object, so this only needs to deliver every pixel of what the user selected.
 */
expect suspend fun cropToSquareJpeg(
    bytes: ByteArray,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    outputSize: Int = 1024,
    quality: Int = 92,
): ByteArray
