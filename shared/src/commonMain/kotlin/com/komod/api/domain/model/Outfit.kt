package com.komod.api.domain.model

data class Outfit(
    val id: String,
    val name: String,
    val reason: String,
    val matchScore: Int,
    val wardrobeItemIds: List<String>,
    val items: List<OutfitItem>,
)
