package com.komod.api.platform

class ImagePickerLauncher internal constructor(
    private val galleryAction: () -> Unit,
    private val cameraAction: () -> Unit,
) {
    fun launchGallery() = galleryAction()

    fun launchCamera() = cameraAction()
}
