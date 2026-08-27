package com.komod.api.data.api.model

import kotlinx.serialization.Serializable

@Serializable
data class SelectedOutfitItemsDto(
    val top: String? = null,
    val bottom: String? = null,
    val shoes: String? = null,
)
