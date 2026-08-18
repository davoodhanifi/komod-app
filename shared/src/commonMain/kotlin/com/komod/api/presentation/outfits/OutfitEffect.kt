package com.komod.api.presentation.outfits

sealed interface OutfitEffect {
    data class ShowSnackbar(val message: String) : OutfitEffect
    // A generation succeeded, changing today's outfit-generation usage count — distinct
    // from ShowSnackbar so callers can hook a usage-dependent refresh (e.g. Profile's
    // subscription usage) without parsing message text.
    data object OutfitsGenerated : OutfitEffect
}
