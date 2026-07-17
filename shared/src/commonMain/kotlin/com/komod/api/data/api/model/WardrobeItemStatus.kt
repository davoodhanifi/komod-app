package com.komod.api.data.api.model

import kotlinx.serialization.Serializable

@Serializable
enum class WardrobeItemStatus {
    Active,
    Archived,
    Sold,
    Donated,
    Rented,
    Hidden,
}
