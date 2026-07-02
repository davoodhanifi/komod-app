package com.komod.api.presentation.outfits

import com.komod.api.domain.model.Outfit
import com.komod.api.domain.model.OutfitOccasion
import com.komod.api.domain.model.OutfitStyle

data class OutfitUiState(
    val selectedOccasion: OutfitOccasion = OutfitOccasion.All,
    val selectedStyle: OutfitStyle = OutfitStyle.Casual,
    val outfits: List<Outfit> = emptyList(),
    val isGenerating: Boolean = false,
    val errorMessage: String? = null,
)
