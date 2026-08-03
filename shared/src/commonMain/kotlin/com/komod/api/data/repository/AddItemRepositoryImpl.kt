package com.komod.api.data.repository

import com.komod.api.data.api.WardrobeApiService
import com.komod.api.data.api.model.CreateImageResponse
import com.komod.api.data.api.model.ImageStatus
import com.komod.api.data.api.model.toImageStatus
import com.komod.api.data.storage.StorageService
import com.komod.api.domain.model.UploadedImage
import com.komod.api.platform.createThumbnail
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// The thumbnail upload is comparatively small/fast, so most of the reported
// progress is attributed to the original image upload.
private const val OriginalUploadWeight = 0.8f

class AddItemRepositoryImpl(
    private val wardrobeApiService: WardrobeApiService,
    private val storageService: StorageService,
    private val uploadedImageStore: UploadedImageStore,
) : AddItemRepository {
    // Lives as long as this singleton (the app process), not any particular
    // screen, so background analysis survives the caller navigating away.
    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

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

    override fun triggerAnalysisInBackground(imageId: String) {
        backgroundScope.launch {
            runCatching { analyzeWardrobeItems(imageId) }
                .onFailure { error ->
                    // The image stays in the local Upload Queue as Pending; retry lands later.
                    println("AddItemRepository: failed to trigger analysis for image $imageId: ${error.message}")
                }
        }
    }

    override fun saveUploadedImage(image: CreateImageResponse) {
        uploadedImageStore.add(
            UploadedImage(
                imageId = image.imageId,
                storagePath = image.storagePath,
                thumbnailStoragePath = image.thumbnailStoragePath,
                status = ImageStatus.Pending,
            ),
        )
    }

    override val uploadedImages: StateFlow<List<UploadedImage>> = uploadedImageStore.uploadedImages

    override suspend fun getThumbnailUrl(storagePath: String): String? = storageService.createSignedUrl(storagePath)

    override suspend fun refreshUploadedImages() {
        val remoteImages = wardrobeApiService.getUploadedImages().map { dto ->
            UploadedImage(
                imageId = dto.imageId,
                storagePath = dto.originalImagePath,
                thumbnailStoragePath = dto.thumbnailStoragePath,
                status = dto.status.toImageStatus(),
            )
        }
        uploadedImageStore.upsertAll(remoteImages)
    }
}
