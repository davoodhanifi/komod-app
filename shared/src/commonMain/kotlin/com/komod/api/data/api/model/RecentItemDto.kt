package com.komod.api.data.api.model

import kotlinx.serialization.Serializable

@Serializable
data class RecentItemDto(
    val id: String,
    val itemName: String?,
    val croppedImageStoragePath: String?,
    val createdAt: String,
)
