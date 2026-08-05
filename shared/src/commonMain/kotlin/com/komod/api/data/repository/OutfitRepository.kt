package com.komod.api.data.repository

import com.komod.api.domain.model.Outfit
import com.komod.api.domain.model.WeatherCurrent

interface OutfitRepository {
    suspend fun generateOutfits(
        occasion: String,
        style: String? = null,
        weather: WeatherCurrent? = null,
    ): List<Outfit>

    suspend fun saveOutfit(outfit: Outfit)
}
