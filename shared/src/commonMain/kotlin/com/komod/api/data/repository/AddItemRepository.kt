package com.komod.api.data.repository

import com.komod.api.data.api.model.CreateImageResponse

interface AddItemRepository {
    suspend fun createImage(): CreateImageResponse

    suspend fun uploadImage(
        storagePath: String,
        bytes: ByteArray,
        mimeType: String,
        onProgress: (Float) -> Unit,
    )

    suspend fun analyzeWardrobeItems(imageId: String)
}
