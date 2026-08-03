package com.komod.api.data.repository

import com.komod.api.data.api.WardrobeApiService
import com.komod.api.data.api.model.CreateImageResponse
import com.komod.api.data.storage.StorageService
import com.komod.api.platform.createThumbnail

// The thumbnail upload is comparatively small/fast, so most of the reported
// progress is attributed to the original image upload.
private const val OriginalUploadWeight = 0.8f

class AddItemRepositoryImpl(
    private val wardrobeApiService: WardrobeApiService,
    private val storageService: StorageService,
    private val uploadedImageStore: UploadedImageStore,
) : AddItemRepository {
    override suspend fun createImage(): CreateImageResponse = wardrobeApiService.createImage()

    override suspend fun uploadImage(
        image: CreateImageResponse,
        bytes: ByteArray,
        mimeType: String,
        onProgress: (Float) -> Unit,
    ) {
        storageService.upload(
            storagePath = image.storagePath,
            bytes = bytes,
            mimeType = mimeType,
            onProgress = { fraction -> onProgress(fraction * OriginalUploadWeight) },
        )

        val thumbnailBytes = createThumbnail(bytes)
        storageService.upload(
            storagePath = image.thumbnailStoragePath,
            bytes = thumbnailBytes,
            mimeType = "image/jpeg",
            onProgress = { fraction -> onProgress(OriginalUploadWeight + fraction * (1f - OriginalUploadWeight)) },
        )
    }

    override suspend fun analyzeWardrobeItems(imageId: String) {
        wardrobeApiService.analyzeWardrobeItems(imageId)
    }

    override fun saveUploadedImage(image: CreateImageResponse) {
        uploadedImageStore.add(image)
    }
}
