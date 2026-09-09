package com.komod.api.presentation.savedoutfits

import com.komod.api.domain.model.SavedOutfit

sealed interface SavedOutfitsUiState {
    data object Loading : SavedOutfitsUiState

    data class Success(
        val outfits: List<SavedOutfit>,
        val isLoadingMore: Boolean = false,
        val hasNextPage: Boolean = false,
    ) : SavedOutfitsUiState

    data class Error(val message: String) : SavedOutfitsUiState
}
