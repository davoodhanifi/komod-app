package com.komod.api.data.repository

import com.komod.api.domain.model.Outfit
import com.komod.api.domain.model.OutfitItem
import com.komod.api.domain.model.OutfitOfTheDay
import com.komod.api.domain.model.WeatherCurrent

interface OutfitRepository {
    suspend fun generateOutfits(
        occasion: String,
        style: String? = null,
        weather: WeatherCurrent? = null,
        // Wardrobe item ids the user picked as starting points, or null for "generate
        // from scratch" — passed through to the backend untouched, one per optional slot.
        selectedTopId: String? = null,
        selectedBottomId: String? = null,
        selectedShoesId: String? = null,
    ): List<Outfit>

    // Backend-driven weather-aware Outfit of the Day: the backend regenerates this on its
    // own fixed 6-hour local-time schedule and returns the same cached result (outfits and
    // weather snapshot together) for repeated calls within the same window.
    suspend fun getOutfitOfTheDay(latitude: Double, longitude: Double): OutfitOfTheDay

    suspend fun saveOutfit(outfit: Outfit): String

    suspend fun unsaveOutfit(savedOutfitId: String)

    // Resolves item images/categories from stable wardrobe item URLs rather than the
    // short-lived (1h) signed URLs generateOutfits() returns — safe to call for outfits
    // that were generated earlier and are being displayed again later.
    suspend fun hydrateOutfitItems(wardrobeItemIds: List<String>): List<OutfitItem>
}
