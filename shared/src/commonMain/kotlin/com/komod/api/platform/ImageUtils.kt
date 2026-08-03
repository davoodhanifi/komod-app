package com.komod.api.platform

import androidx.compose.ui.graphics.ImageBitmap

expect fun ByteArray.toImageBitmap(): ImageBitmap

expect suspend fun createThumbnail(bytes: ByteArray, maxDimension: Int = 512): ByteArray
