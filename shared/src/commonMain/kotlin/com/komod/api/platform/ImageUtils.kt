package com.komod.api.platform

import androidx.compose.ui.graphics.ImageBitmap

expect fun ByteArray.toImageBitmap(): ImageBitmap
