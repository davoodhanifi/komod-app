package com.komod.api.platform

import androidx.compose.runtime.Composable

@Composable
expect fun rememberImagePickerLauncher(
    onResult: (bytes: ByteArray, mimeType: String) -> Unit,
    onCancel: () -> Unit = {},
): ImagePickerLauncher
