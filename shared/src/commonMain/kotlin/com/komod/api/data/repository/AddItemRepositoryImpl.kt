package com.komod.api.data.repository

import com.komod.api.core.error.AppLogger
import com.komod.api.core.error.ErrorContext
import com.komod.api.core.error.ErrorMapper
import com.komod.api.data.api.WardrobeApiService
import com.komod.api.data.api.model.CreateImageResponse
import com.komod.api.data.api.model.ImageStatus
import com.komod.api.data.storage.StorageService
import com.komod.api.domain.model.UploadedImage
import com.komod.api.platform.createThumbnail
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.ContentConvertException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException

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
            try {
                val result = wardrobeApiService.analyzeWardrobeItems(imageId)
                // Reflect the queued status immediately rather than waiting for the
                // next poll tick, so the Upload Queue can show "Extracting items..."
                // as soon as the backend confirms it queued the work.
                uploadedImageStore.updateStatus(imageId, result.status)
            } catch (error: ContentConvertException) {
                // Ktor's ContentNegotiation wraps a JSON decoding failure in this type
                // (not kotlinx.serialization.SerializationException, which is caught
                // separately below in case anything ever throws that one directly) — the
                // HTTP call itself succeeded, the backend accepted and almost certainly
                // queued the job, but the response body didn't match the expected shape.
                // A version-skew bug exactly like this is what caused the original
                // incident, so it's logged explicitly and distinctly here rather than
                // folded into the generic branch below, and the image is still promoted
                // to Processing since the request did succeed.
                logParseFailureAndPromoteToProcessing(imageId, error)
            } catch (error: SerializationException) {
                logParseFailureAndPromoteToProcessing(imageId, error)
            } catch (error: ResponseException) {
                if (error.response.status == HttpStatusCode.PreconditionFailed) {
                    // The backend already has this image queued/processing (e.g. a
                    // retried request raced with an earlier one) — treat that as
                    // already-Processing rather than a failure, and let the existing
                    // poll loop pick up whatever status it actually reaches.
                    uploadedImageStore.updateStatus(imageId, ImageStatus.Processing)
                } else {
                    // Any other failure leaves the image at its last known status
                    // (Pending) in the local Upload Queue; the next poll from the
                    // Wardrobe screen recovers once the backend is healthy again.
                    ErrorMapper.toUserMessage(error, tag = "AddItemRepository", context = ErrorContext.AiProcessing)
                }
            } catch (error: kotlinx.io.IOException) {
                ErrorMapper.toUserMessage(error, tag = "AddItemRepository", context = ErrorContext.AiProcessing)
            }
        }
    }

    private fun logParseFailureAndPromoteToProcessing(imageId: String, error: Throwable) {
        // A logging failure must never propagate out of here — this runs inside a
        // fire-and-forget coroutine, and an uncaught exception here would silently
        // discard the outcome without ever updating the store.
        runCatching { AppLogger.e("AddItemRepository", "Failed to parse analyze response for image $imageId", error) }
        uploadedImageStore.updateStatus(imageId, ImageStatus.Processing)
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

    override fun removeUploadedImage(imageId: String) {
        uploadedImageStore.remove(imageId)
    }

    override suspend fun deleteUploadedImage(imageId: String) {
        try {
            wardrobeApiService.deleteUploadedImage(imageId)
            uploadedImageStore.remove(imageId)
        } catch (error: ResponseException) {
            when (error.response.status) {
                HttpStatusCode.NotFound -> throw UploadedImageDeleteNotFoundException()
                HttpStatusCode.Forbidden -> throw UploadedImageDeleteForbiddenException()
                HttpStatusCode.PreconditionFailed -> throw UploadedImageDeleteConflictException()
                else -> throw UploadedImageDeleteNetworkException(error)
            }
        } catch (error: kotlinx.io.IOException) {
            throw UploadedImageDeleteNetworkException(error)
        }
    }

    override val uploadedImages: StateFlow<List<UploadedImage>> = uploadedImageStore.uploadedImages

    override suspend fun getThumbnailUrl(storagePath: String): String? = storageService.createSignedUrl(storagePath)

    override suspend fun refreshUploadedImages() {
        val remoteImages = wardrobeApiService.getUploadedImages().map { dto ->
            UploadedImage(
                imageId = dto.imageId,
                storagePath = dto.originalImagePath,
                thumbnailStoragePath = dto.thumbnailStoragePath,
                status = dto.status,
                uploadedAt = dto.uploadedAt,
                extractedItemCount = dto.extractedItemCount,
            )
        }
        uploadedImageStore.upsertAll(remoteImages)
    }
}
