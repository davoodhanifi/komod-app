package com.komod.api.data.api.model

import kotlinx.serialization.Serializable

@Serializable
data class WardrobeItemUpdateRequest(
    val category: String? = null,
    val subcategory: String? = null,
    val name: String? = null,
    val brand: String? = null,
    val material: String? = null,
    val primaryColor: String? = null,
    val formality: String? = null,
    val season: List<String>? = null,
    val occasion: List<String>? = null,
    val status: Int? = null,
)
