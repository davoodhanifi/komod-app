@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.komod.api.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusDenied
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVAuthorizationStatusRestricted
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.Foundation.NSData
import platform.Foundation.NSItemProvider
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.UIKit.UIAlertAction
import platform.UIKit.UIAlertActionStyleDefault
import platform.UIKit.UIAlertController
import platform.UIKit.UIAlertControllerStyleAlert
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.posix.memcpy

// UIImagePickerControllerSourceType enum values via enum members
private val SourceTypeCamera =
    UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera

private var activeImagePickerDelegate: NSObject? = null
private var activePHPickerDelegate: NSObject? = null

@Composable
actual fun rememberImagePickerLauncher(
    maxSelection: Int,
    onResult: (images: List<PickedImage>) -> Unit,
    onCancel: () -> Unit,
): ImagePickerLauncher {
    val onResultState = rememberUpdatedState(onResult)
    val onCancelState = rememberUpdatedState(onCancel)

    return remember {
        ImagePickerLauncher(
            galleryAction = {
                presentMultiImagePicker(
                    maxSelection = maxSelection,
                    onResult = { images -> onResultState.value(images) },
                    onCancel = { onCancelState.value() },
                )
            },
            cameraAction = {
                launchCameraPicker(
                    onResult = { bytes, mimeType -> onResultState.value(listOf(PickedImage(bytes, mimeType))) },
                    onCancel = { onCancelState.value() },
                )
            },
        )
    }
}

private fun launchCameraPicker(
    onResult: (ByteArray, String) -> Unit,
    onCancel: () -> Unit,
) {
    if (!UIImagePickerController.isSourceTypeAvailable(SourceTypeCamera)) {
        showAlert(
            title = "Camera unavailable",
            message = "This device does not have a camera available.",
        )
        onCancel()
        return
    }
    when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
        AVAuthorizationStatusAuthorized -> {
            presentImagePicker(SourceTypeCamera, onResult, onCancel)
        }

        AVAuthorizationStatusNotDetermined -> {
            AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                runOnMain {
                    if (granted) {
                        presentImagePicker(SourceTypeCamera, onResult, onCancel)
                    } else {
                        showAlert(
                            title = "Camera access denied",
                            message = "Enable camera access in Settings to take a photo.",
                        )
                        onCancel()
                    }
                }
            }
        }

        AVAuthorizationStatusDenied, AVAuthorizationStatusRestricted -> {
            showAlert(
                title = "Camera access denied",
                message = "Enable camera access in Settings to take a photo.",
            )
            onCancel()
        }

        else -> {
            onCancel()
        }
    }
}

private fun presentImagePicker(
    sourceType: UIImagePickerControllerSourceType,
    onResult: (ByteArray, String) -> Unit,
    onCancel: () -> Unit,
) {
    runOnMain {
        val topViewController = topViewController() ?: run {
            onCancel()
            return@runOnMain
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
}

private fun presentMultiImagePicker(
    maxSelection: Int,
    onResult: (List<PickedImage>) -> Unit,
    onCancel: () -> Unit,
) {
    runOnMain {
        val topViewController = topViewController() ?: run {
            onCancel()
            return@runOnMain
        }

        val configuration = PHPickerConfiguration().apply {
            selectionLimit = maxSelection.toLong()
            filter = PHPickerFilter.imagesFilter()
        }
        val pickerViewController = PHPickerViewController(configuration = configuration)

        val delegate = object : NSObject(), PHPickerViewControllerDelegateProtocol {
            override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
                picker.dismissViewControllerAnimated(true, null)
                activePHPickerDelegate = null

                val results = didFinishPicking.filterIsInstance<PHPickerResult>()
                if (results.isEmpty()) {
                    onCancel()
                    return
                }

                val images = arrayOfNulls<PickedImage>(results.size)
                var remaining = results.size

                results.forEachIndexed { index, result ->
                    val provider = result.itemProvider
                    provider.loadDataRepresentationForTypeIdentifier("public.image") { data, _ ->
                        runOnMain {
                            images[index] = data?.let { PickedImage(it.toByteArray(), mimeTypeFor(provider)) }
                            remaining -= 1
                            if (remaining == 0) {
                                val picked = images.filterNotNull()
                                if (picked.isEmpty()) onCancel() else onResult(picked)
                            }
                        }
                    }
                }
            }
        }
        activePHPickerDelegate = delegate
        pickerViewController.delegate = delegate
        topViewController.presentViewController(pickerViewController, true, null)
    }
}

private fun mimeTypeFor(provider: NSItemProvider): String {
    val identifier = provider.registeredTypeIdentifiers.firstOrNull() as? String ?: return "image/jpeg"
    return when {
        identifier.contains("png") -> "image/png"
        identifier.contains("heic") || identifier.contains("heif") -> "image/heic"
        else -> "image/jpeg"
    }
}

private fun showAlert(title: String, message: String) {
    runOnMain {
        val topViewController = topViewController() ?: return@runOnMain
        val alert = UIAlertController.alertControllerWithTitle(
            title = title,
            message = message,
            preferredStyle = UIAlertControllerStyleAlert,
        )
        alert.addAction(UIAlertAction.actionWithTitle("OK", UIAlertActionStyleDefault, null))
        topViewController.presentViewController(alert, true, null)
    }
}

private fun runOnMain(block: () -> Unit) {
    dispatch_async(dispatch_get_main_queue(), block)
}

private fun topViewController(): UIViewController? {
    val keyWindow = UIApplication.sharedApplication.windows
        .firstOrNull { window ->
            (window as? UIWindow)?.isKeyWindow() == true
        } as? UIWindow ?: UIApplication.sharedApplication.keyWindow
    return topViewController(from = keyWindow?.rootViewController)
}

private fun topViewController(from: UIViewController?): UIViewController? {
    if (from == null) return null
    val presented = from.presentedViewController
    return if (presented != null) topViewController(from = presented) else from
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
