package com.komod.api.data.api.model

import kotlinx.serialization.Serializable

@Serializable
data class OutfitGenerateResponse(
    val outfits: List<OutfitGeneratedDto>,
)

@Serializable
data class OutfitGeneratedDto(
    val name: String,
    val reason: String,
    val wardrobeItemIds: List<String>,
    val items: List<OutfitGeneratedItemDto> = emptyList(),
)

@Serializable
data class OutfitGeneratedItemDto(
    val imageUrl: String,
)
