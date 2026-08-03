package com.komod.api.data.repository

import com.komod.api.data.api.model.CreateImageResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// Holds images uploaded during this app session so the Upload Queue can render
// thumbnails immediately once built, without waiting on another network call.
class UploadedImageStore {
    private val _uploadedImages = MutableStateFlow<List<CreateImageResponse>>(emptyList())
    val uploadedImages: StateFlow<List<CreateImageResponse>> = _uploadedImages.asStateFlow()

    fun add(image: CreateImageResponse) {
        _uploadedImages.update { current ->
            if (current.any { it.imageId == image.imageId }) current else current + image
        }
    }
}
