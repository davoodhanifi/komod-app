package com.komod.api.data.repository

import com.komod.api.domain.model.UploadedImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// Holds images uploaded during this app session, reconciled with the backend's
// upload queue, so the Upload Queue can render thumbnails and status immediately
// without waiting on another network call.
class UploadedImageStore {
    private val _uploadedImages = MutableStateFlow<List<UploadedImage>>(emptyList())
    val uploadedImages: StateFlow<List<UploadedImage>> = _uploadedImages.asStateFlow()

    fun add(image: UploadedImage) {
        upsertAll(listOf(image))
    }

    // Adds unseen images and refreshes known ones (e.g. status changes) in place,
    // without dropping local entries the backend hasn't listed yet.
    fun upsertAll(images: List<UploadedImage>) {
        if (images.isEmpty()) return
        _uploadedImages.update { current ->
            val byId = current.associateByTo(LinkedHashMap()) { it.imageId }
            images.forEach { image -> byId[image.imageId] = image }
            byId.values.toList()
        }
    }

    // A reviewed image drops out of the backend's "awaiting review" list, which
    // upsertAll alone would never reflect, so callers remove it explicitly.
    fun remove(imageId: String) {
        _uploadedImages.update { current -> current.filterNot { it.imageId == imageId } }
    }
}
