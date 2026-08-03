package com.komod.api.presentation.additem

import com.komod.api.platform.PickedImage

sealed interface AddItemUiState {
    data object Initial : AddItemUiState

    data class Uploading(
        val total: Int,
        val completed: Int,
        val progress: Float,
    ) : AddItemUiState

    data class Error(
        val message: String,
        val failedPhotos: List<PickedImage>,
    ) : AddItemUiState
}

sealed interface AddItemEffect {
    data class UploadsSucceeded(val count: Int) : AddItemEffect
    data object SelectionLimitExceeded : AddItemEffect
}
