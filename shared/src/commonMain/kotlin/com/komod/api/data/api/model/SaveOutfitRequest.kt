package com.komod.api.data.api.model

import kotlinx.serialization.Serializable

@Serializable
data class SaveOutfitRequest(
    val name: String,
    val reason: String,
    val wardrobeItemIds: List<String>,
)
