package com.komod.api.domain.model

import com.komod.api.data.api.model.ImageStatus

data class UploadedImage(
    val imageId: String,
    val storagePath: String,
    val thumbnailStoragePath: String,
    val status: ImageStatus,
)
