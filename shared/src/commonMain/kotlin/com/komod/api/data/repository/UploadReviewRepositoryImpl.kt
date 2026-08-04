package com.komod.api.data.repository

import com.komod.api.data.api.WardrobeApiService
import com.komod.api.data.api.model.WardrobeItemDto
import com.komod.api.data.api.model.WardrobeItemReviewRequestItem
import com.komod.api.data.api.model.toDomain
import com.komod.api.data.api.model.toImageStatus
import com.komod.api.data.storage.StorageService
import com.komod.api.domain.model.BoundingBox
import com.komod.api.domain.model.UploadedImageDetail
import com.komod.api.domain.model.WardrobeItemDetail
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode

class UploadReviewRepositoryImpl(
    private val wardrobeApiService: WardrobeApiService,
    private val storageService: StorageService,
) : UploadReviewRepository {
    override suspend fun getUploadedImageDetail(imageId: String): UploadedImageDetail {
        // GET /v1/images/{imageId} 404s on this backend, so the list endpoint
        // (which already powers the Recent Uploads section) is the source of truth.
        val dto = wardrobeApiService.getUploadedImages().find { it.imageId == imageId }
            ?: throw UploadedImageNotFoundException()
        val originalImageUrl = storageService.createSignedUrl(dto.originalImagePath)

        return UploadedImageDetail(
            imageId = dto.imageId,
            status = dto.status.toImageStatus(),
            originalImageUrl = originalImageUrl,
            items = dto.items.map { itemDto -> itemDto.toDetail(fallbackImageUrl = originalImageUrl) },
        )
    }

    override suspend fun submitReview(items: List<WardrobeItemReviewRequestItem>) {
        try {
            wardrobeApiService.reviewWardrobeItems(items)
        } catch (error: ResponseException) {
            when (error.response.status) {
                HttpStatusCode.Unauthorized -> throw UploadReviewUnauthorizedException()
                HttpStatusCode.Forbidden -> throw UploadReviewForbiddenException()
                HttpStatusCode.NotFound -> throw UploadReviewNotFoundException()
                HttpStatusCode.PreconditionFailed -> throw UploadReviewConflictException()
                else -> throw UploadReviewNetworkException(error)
            }
        } catch (error: kotlinx.io.IOException) {
            throw UploadReviewNetworkException(error)
        }
    }

    private suspend fun WardrobeItemDto.toDetail(fallbackImageUrl: String?): WardrobeItemDetail {
        val imageUrl = croppedImageStoragePath
            ?.takeIf { it.isNotBlank() }
            ?.let { path -> storageService.createSignedUrl(path) }
            ?: fallbackImageUrl

        return WardrobeItemDetail(
            id = id,
            imageId = imageId,
            imageUrl = imageUrl,
            status = status,
            category = category,
            subcategory = subcategory,
            itemName = itemName,
            bodyRegion = bodyRegion,
            layer = layer,
            primaryColor = primaryColor,
            secondaryColors = secondaryColors,
            accentColors = accentColors,
            pattern = pattern,
            material = material,
            texture = texture,
            fit = fit,
            silhouette = silhouette,
            sleeveLength = sleeveLength,
            pantLength = pantLength,
            neckline = neckline,
            collarType = collarType,
            closure = closure,
            formality = formality,
            style = style,
            season = season,
            occasion = occasion,
            genderStyle = genderStyle,
            weatherMinTempC = weatherMinTempC,
            weatherMaxTempC = weatherMaxTempC,
            weatherRainFriendly = weatherRainFriendly,
            warmthLevel = warmthLevel,
            features = features,
            recommendedPairings = recommendedPairings,
            embeddingDescription = embeddingDescription,
            isFavorite = isFavorite,
            confidence = confidence,
            createdAt = createdAt,
            boundingBox = boundingBox?.toDomain() ?: BoundingBox.FullImage,
        )
    }
}
