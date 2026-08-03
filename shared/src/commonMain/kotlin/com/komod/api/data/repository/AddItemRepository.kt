package com.komod.api.data.repository

import com.komod.api.data.api.model.CreateImageResponse
import com.komod.api.domain.model.UploadedImage
import kotlinx.coroutines.flow.StateFlow

interface AddItemRepository {
    suspend fun createImage(): CreateImageResponse

    suspend fun uploadImage(
        image: CreateImageResponse,
        bytes: ByteArray,
        mimeType: String,
        onProgress: (Float) -> Unit,
    )

    suspend fun analyzeWardrobeItems(imageId: String)

    // Triggers analysis on a scope that outlives the calling screen, so the
    // request isn't cancelled if the caller navigates away right after upload.
    fun triggerAnalysisInBackground(imageId: String)

    fun saveUploadedImage(image: CreateImageResponse)

    val uploadedImages: StateFlow<List<UploadedImage>>

    suspend fun getThumbnailUrl(storagePath: String): String?

    suspend fun refreshUploadedImages()
}
