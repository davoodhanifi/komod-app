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

@Composable
actual fun rememberImagePickerLauncher(
    onResult: (bytes: ByteArray, mimeType: String) -> Unit,
    onCancel: () -> Unit,
): ImagePickerLauncher {
    val context = LocalContext.current
    val onResultState = rememberUpdatedState(onResult)
    val onCancelState = rememberUpdatedState(onCancel)
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var cameraFile by remember { mutableStateOf<File?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) {
            onCancelState.value()
            return@rememberLauncherForActivityResult
        }

        runCatching {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: error("Unable to read selected image.")
            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            onResultState.value(bytes, mimeType)
        }.onFailure {
            onCancelState.value()
        }
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
            onResultState.value(file.readBytes(), "image/jpeg")
        }.onFailure {
            onCancelState.value()
        }
    }

    fun launchCameraCapture() {
        val file = File.createTempFile("img_", ".jpg", context.cacheDir)
        cameraFile = file
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        cameraUri = uri
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
