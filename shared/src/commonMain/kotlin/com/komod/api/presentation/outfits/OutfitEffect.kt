package com.komod.api.presentation.outfits

sealed interface OutfitEffect {
    data class ShowSnackbar(val message: String) : OutfitEffect
}
