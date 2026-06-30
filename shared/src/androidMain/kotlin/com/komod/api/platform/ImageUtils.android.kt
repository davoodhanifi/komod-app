package com.komod.api.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

actual fun ByteArray.toImageBitmap(): ImageBitmap {
    return android.graphics.BitmapFactory.decodeByteArray(this, 0, size).asImageBitmap()
}
