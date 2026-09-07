package com.komod.api.presentation.wardrobe

import com.komod.api.data.api.model.ImageStatus
import com.komod.api.domain.model.WardrobeItem

sealed interface WardrobeUiState {
    data object Loading : WardrobeUiState

    data class Success(
        val items: List<WardrobeItem>,
        val isLoadingMore: Boolean = false,
        // Whether the backend has more pages beyond what's currently in `items` — used to
        // tell "this category truly has no items" apart from "this category's items just
        // haven't been paginated in yet" when a filter chip (populated upfront from the
        // wardrobe summary) is selected before its items have loaded.
        val hasNextPage: Boolean = false,
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
