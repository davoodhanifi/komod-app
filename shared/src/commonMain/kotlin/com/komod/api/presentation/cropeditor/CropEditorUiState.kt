package com.komod.api.presentation.cropeditor

import com.komod.api.domain.model.CropEditorState

sealed interface CropEditorUiState {
    data object Loading : CropEditorUiState

    data class Ready(
        val itemTitle: String,
        val originalImageUrl: String?,
        val cropState: CropEditorState,
        val isSaving: Boolean = false,
    ) : CropEditorUiState

    data class Error(val message: String) : CropEditorUiState
}
