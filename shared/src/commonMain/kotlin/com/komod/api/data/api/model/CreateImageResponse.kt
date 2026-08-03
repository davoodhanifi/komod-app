package com.komod.api.data.api.model

import kotlinx.serialization.Serializable

@Serializable
data class CreateImageResponse(
    val imageId: String,
    val storagePath: String,
    val thumbnailStoragePath: String,
)
