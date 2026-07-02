package com.komod.api.data.repository

import com.komod.api.domain.model.Outfit

interface OutfitRepository {
    suspend fun generateOutfits(occasion: String, style: String): List<Outfit>
}
