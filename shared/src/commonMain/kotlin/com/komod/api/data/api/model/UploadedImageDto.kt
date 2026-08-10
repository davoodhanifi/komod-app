package com.komod.api.data.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UploadedImageDto(
    @SerialName("id") val imageId: String,
    val uploadedAt: String,
    @SerialName("storagePath") val originalImagePath: String,
    val thumbnailStoragePath: String,
    val status: ImageStatus,
    @SerialName("itemCount") val extractedItemCount: Int,
    val items: List<WardrobeItemDto> = emptyList(),
)
