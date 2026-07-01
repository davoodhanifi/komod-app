package com.komod.api.presentation.wardrobe

import com.komod.api.domain.model.WardrobeItem

sealed interface WardrobeUiState {
    data object Loading : WardrobeUiState

    data class Success(
        val items: List<WardrobeItem>,
    ) : WardrobeUiState

    data class Error(
        val message: String,
    ) : WardrobeUiState
}
