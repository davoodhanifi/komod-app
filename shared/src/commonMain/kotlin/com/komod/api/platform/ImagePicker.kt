package com.komod.api.platform

import androidx.compose.runtime.Composable

data class PickedImage(
    val bytes: ByteArray,
    val mimeType: String,
)

@Composable
expect fun rememberImagePickerLauncher(
    maxSelection: Int = 5,
    onResult: (images: List<PickedImage>) -> Unit,
    onCancel: () -> Unit = {},
): ImagePickerLauncher
