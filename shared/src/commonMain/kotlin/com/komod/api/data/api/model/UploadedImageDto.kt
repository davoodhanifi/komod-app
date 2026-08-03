package com.komod.api.data.api.model

import kotlinx.serialization.Serializable

@Serializable
data class UploadedImageDto(
    val imageId: String,
    val uploadedAt: String,
    val originalImagePath: String,
    val thumbnailStoragePath: String,
    val status: Int,
    val extractedItemCount: Int,
    val items: List<WardrobeItemDto> = emptyList(),
)
