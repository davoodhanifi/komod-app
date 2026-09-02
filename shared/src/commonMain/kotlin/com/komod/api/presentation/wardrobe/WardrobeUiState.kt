package com.komod.api.presentation.wardrobe

import com.komod.api.data.api.model.ImageStatus
import com.komod.api.domain.model.WardrobeItem

sealed interface WardrobeUiState {
    data object Loading : WardrobeUiState

    data class Success(
        val items: List<WardrobeItem>,
        val isLoadingMore: Boolean = false,
    ) : WardrobeUiState

    data class Error(
        val message: String,
    ) : WardrobeUiState
}

data class RecentUploadUi(
    val imageId: String,
    val thumbnailUrl: String?,
    val status: ImageStatus,
)
