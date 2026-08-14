package com.komod.api.platform

import androidx.compose.runtime.Composable

/**
 * A photo picked from the gallery or camera. [bytes] is loaded lazily (and cached) via
 * [loadBytes] instead of being read up front, so selecting several photos from the gallery can
 * hand them off immediately — the actual file read only happens once something (a thumbnail, the
 * crop editor, or the upload step) actually needs the bytes.
 */
class PickedImage(
    val mimeType: String,
    private val loadBytes: suspend () -> ByteArray,
) {
    private var cachedBytes: ByteArray? = null

    suspend fun bytes(): ByteArray = cachedBytes ?: loadBytes().also { cachedBytes = it }
}

@Composable
expect fun rememberImagePickerLauncher(
    maxSelection: Int = 5,
    onResult: (images: List<PickedImage>) -> Unit,
    onCancel: () -> Unit = {},
): ImagePickerLauncher
