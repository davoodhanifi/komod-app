package com.komod.api.data.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WardrobeFavoriteRequest(
    val favorite: Boolean,
    @SerialName("Favorite")
    val favoritePascalCase: Boolean = favorite,
)
