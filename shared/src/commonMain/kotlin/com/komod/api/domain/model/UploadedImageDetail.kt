package com.komod.api.domain.model

import com.komod.api.data.api.model.ImageStatus

data class UploadedImageDetail(
    val imageId: String,
    val status: ImageStatus,
    val originalImageUrl: String?,
    val items: List<WardrobeItemDetail>,
)
