package com.komod.api.data.api.model

import kotlinx.serialization.Serializable

@Serializable
data class OutfitDto(
    val id: String,
    val name: String,
    val reason: String,
    val isFavorite: Boolean = false,
    val wardrobeItems: List<OutfitWardrobeItemDto> = emptyList(),
    val createdAt: String,
)
