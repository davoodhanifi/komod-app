@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.komod.api.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIViewController
import platform.darwin.NSObject
import platform.posix.memcpy

// UIImagePickerControllerSourceType enum values via enum members
private val SourceTypePhotoLibrary =
    UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary
private val SourceTypeCamera =
    UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera

private var activeImagePickerDelegate: NSObject? = null

@Composable
actual fun rememberImagePickerLauncher(
    onResult: (bytes: ByteArray, mimeType: String) -> Unit,
    onCancel: () -> Unit,
): ImagePickerLauncher {
    val onResultState = rememberUpdatedState(onResult)
    val onCancelState = rememberUpdatedState(onCancel)

    return remember {
        ImagePickerLauncher(
            galleryAction = {
                presentImagePicker(
                    sourceType = SourceTypePhotoLibrary,
                    onResult = { bytes, mimeType -> onResultState.value(bytes, mimeType) },
                    onCancel = { onCancelState.value() },
                )
            },
            cameraAction = {
                val sourceType = if (UIImagePickerController.isSourceTypeAvailable(SourceTypeCamera)) {
                    SourceTypeCamera
                } else {
                    SourceTypePhotoLibrary
                }
                presentImagePicker(
                    sourceType = sourceType,
                    onResult = { bytes, mimeType -> onResultState.value(bytes, mimeType) },
                    onCancel = { onCancelState.value() },
                )
            },
        )
    }
}

private fun presentImagePicker(
    sourceType: UIImagePickerControllerSourceType,
    onResult: (ByteArray, String) -> Unit,
    onCancel: () -> Unit,
) {
    val topViewController = topViewController() ?: run {
        onCancel()
        return
    }
    val picker = UIImagePickerController().apply {
        this.sourceType = sourceType
    }
    val delegate = object : NSObject(), UIImagePickerControllerDelegateProtocol,
        UINavigationControllerDelegateProtocol {

        override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
            picker.dismissViewControllerAnimated(true, null)
            activeImagePickerDelegate = null
            onCancel()
        }

        override fun imagePickerController(
            picker: UIImagePickerController,
            didFinishPickingMediaWithInfo: Map<Any?, *>,
        ) {
            val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
            val bytes = image?.let { UIImageJPEGRepresentation(it, 0.85)?.toByteArray() }
            picker.dismissViewControllerAnimated(true, null)
            activeImagePickerDelegate = null
            if (bytes == null) {
                onCancel()
            } else {
                onResult(bytes, "image/jpeg")
            }
        }
    }
    activeImagePickerDelegate = delegate
    picker.delegate = delegate
    topViewController.presentViewController(picker, true, null)
}

private fun topViewController(): UIViewController? {
    var controller = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (controller?.presentedViewController != null) {
        controller = controller.presentedViewController
    }
    return controller
}

private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size == 0) return ByteArray(0)
    return ByteArray(size).also { array ->
        array.usePinned { pinned ->
            memcpy(pinned.addressOf(0), bytes, length)
        }
    }
}
