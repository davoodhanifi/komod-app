package com.komod.api.data.repository

import com.komod.api.data.api.model.ImageStatus
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
    // without dropping local entries the backend hasn't listed yet. Genuinely new
    // images are prepended (newest upload first, at the left of the Recent Uploads
    // row) rather than appended — otherwise a new upload lands at the end of a long
    // queue, off-screen until the user manually scrolls to it.
    fun upsertAll(images: List<UploadedImage>) {
        if (images.isEmpty()) return
        _uploadedImages.update { current ->
            val incomingById = images.associateBy { it.imageId }
            val currentIds = current.mapTo(mutableSetOf()) { it.imageId }
            val updatedCurrent = current.map { existing -> incomingById[existing.imageId] ?: existing }
            val newEntries = images.filterNot { it.imageId in currentIds }
            newEntries + updatedCurrent
        }
    }

    // A reviewed image drops out of the backend's "awaiting review" list, which
    // upsertAll alone would never reflect, so callers remove it explicitly.
    fun remove(imageId: String) {
        _uploadedImages.update { current -> current.filterNot { it.imageId == imageId } }
    }

    // Reflects a status the client already knows (e.g. the queued response from
    // POST /wardrobe-items/analyze) immediately, without waiting for the next
    // GET /images/uploaded poll tick. A no-op if the image isn't tracked yet.
    fun updateStatus(imageId: String, status: ImageStatus) {
        _uploadedImages.update { current ->
            current.map { image -> if (image.imageId == imageId) image.copy(status = status) else image }
        }
    }
}
