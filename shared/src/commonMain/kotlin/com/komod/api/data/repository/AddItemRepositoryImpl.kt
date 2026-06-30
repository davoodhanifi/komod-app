package com.komod.api.data.repository

import com.komod.api.data.api.WardrobeApiService
import com.komod.api.data.api.model.CreateImageResponse
import com.komod.api.data.storage.StorageService

class AddItemRepositoryImpl(
    private val wardrobeApiService: WardrobeApiService,
    private val storageService: StorageService,
) : AddItemRepository {
    override suspend fun createImage(): CreateImageResponse = wardrobeApiService.createImage()

    override suspend fun uploadImage(
        storagePath: String,
        bytes: ByteArray,
        mimeType: String,
        onProgress: (Float) -> Unit,
    ) {
        storageService.upload(
            storagePath = storagePath,
            bytes = bytes,
            mimeType = mimeType,
            onProgress = onProgress,
        )
    }

    override suspend fun analyzeWardrobeItems(imageId: String) {
        wardrobeApiService.analyzeWardrobeItems(imageId)
    }
}
