package com.komod.api.presentation.wardrobe

import com.komod.api.domain.model.WardrobeItemDetail

sealed interface WardrobeItemDetailUiState {
    data object Loading : WardrobeItemDetailUiState
    data class Success(val item: WardrobeItemDetail) : WardrobeItemDetailUiState
    data class Error(val message: String) : WardrobeItemDetailUiState
}
