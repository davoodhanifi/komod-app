package com.komod.api.data.api.model

import kotlinx.serialization.Serializable

@Serializable
data class ImageDto(
    val imageId: String,
    val storagePath: String,
)
