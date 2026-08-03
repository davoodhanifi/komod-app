package com.komod.api.presentation.uploadreview

import com.komod.api.domain.model.UploadedImageDetail

sealed interface UploadReviewUiState {
    data object Loading : UploadReviewUiState

    data class Ready(
        val detail: UploadedImageDetail,
        val selectedItemIds: Set<String>,
        val isSubmitting: Boolean = false,
    ) : UploadReviewUiState

    data class Error(val message: String) : UploadReviewUiState
}
