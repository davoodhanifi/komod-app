package com.komod.api.data.repository

import com.komod.api.domain.model.Outfit
import com.komod.api.domain.model.OutfitItem
import com.komod.api.domain.model.WeatherCurrent

interface OutfitRepository {
    suspend fun generateOutfits(
        occasion: String,
        style: String? = null,
        weather: WeatherCurrent? = null,
    ): List<Outfit>

    suspend fun saveOutfit(outfit: Outfit): String

    suspend fun unsaveOutfit(savedOutfitId: String)

    // Resolves item images/categories from stable wardrobe item URLs rather than the
    // short-lived (1h) signed URLs generateOutfits() returns — safe to call for outfits
    // that were generated earlier and are being displayed again later.
    suspend fun hydrateOutfitItems(wardrobeItemIds: List<String>): List<OutfitItem>
}
