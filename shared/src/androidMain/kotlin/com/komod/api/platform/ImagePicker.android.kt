package com.komod.api.platform

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
actual fun rememberImagePickerLauncher(
    maxSelection: Int,
    onResult: (images: List<PickedImage>) -> Unit,
    onCancel: () -> Unit,
): ImagePickerLauncher {
    val context = LocalContext.current
    val onResultState = rememberUpdatedState(onResult)
    val onCancelState = rememberUpdatedState(onCancel)
    var cameraFile by remember { mutableStateOf<File?>(null) }

    // Wraps [uri] immediately without reading its bytes — the actual content-resolver read only
    // happens lazily, off the main thread, the first time something needs this photo's bytes.
    fun pickedImage(uri: Uri): PickedImage {
        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
        return PickedImage(mimeType) {
            withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: error("Unable to read selected image.")
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxSelection),
    ) { uris ->
        if (uris.isEmpty()) {
            onCancelState.value()
            return@rememberLauncherForActivityResult
        }

        onResultState.value(uris.take(maxSelection).map(::pickedImage))
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        val file = cameraFile
        if (!success || file == null) {
            onCancelState.value()
            return@rememberLauncherForActivityResult
        }

        runCatching {
            val bytes = file.readBytes()
            onResultState.value(listOf(PickedImage("image/jpeg") { bytes }))
        }.onFailure {
            onCancelState.value()
        }
    }

    fun launchCameraCapture() {
        val file = File.createTempFile("img_", ".jpg", context.cacheDir)
        cameraFile = file
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        cameraLauncher.launch(uri)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            launchCameraCapture()
        } else {
            onCancelState.value()
        }
    }

    return remember(context, galleryLauncher, cameraLauncher, permissionLauncher) {
        ImagePickerLauncher(
            galleryAction = {
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
            cameraAction = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    launchCameraCapture()
                } else {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            },
        )
    }
}
