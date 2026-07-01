package com.komod.api.domain.model

data class WardrobeItem(
    val id: String,
    val imageId: String,
    val itemName: String?,
    val category: String,
    val subcategory: String?,
    val primaryColor: String?,
    val dominantColorHex: String?,
    val style: List<String>?,
    val season: List<String>?,
    val occasion: List<String>?,
    val material: String?,
    val formality: String?,
    val imageUrl: String?,
    val createdAt: String,
)
