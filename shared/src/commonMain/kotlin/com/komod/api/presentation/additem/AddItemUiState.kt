package com.komod.api.presentation.additem

sealed interface AddItemUiState {
    data object Initial : AddItemUiState

    /**
     * Photos have been picked but not uploaded yet. [activeCropPhotoId] non-null means the crop
     * editor is showing for that photo; null means the review grid (or, for a single photo, the
     * crop editor itself) is showing. [savingCropPhotoId] non-null means that photo's crop is
     * being rasterized in the background — the crop editor stays up with its buttons disabled
     * until it resolves, so the user always sees the actual cropped result before moving on.
     */
    data class Reviewing(
        val photos: List<ReviewPhoto>,
        val activeCropPhotoId: String? = null,
        val savingCropPhotoId: String? = null,
    ) : AddItemUiState

    data class Uploading(
        val total: Int,
        val completed: Int,
        val progress: Float,
    ) : AddItemUiState

    data class Error(
        val message: String,
        val failedPhotos: List<ReviewPhoto>,
    ) : AddItemUiState
}

sealed interface AddItemEffect {
    data class UploadsSucceeded(val count: Int) : AddItemEffect
    data object SelectionLimitExceeded : AddItemEffect
    data object CropFailed : AddItemEffect
}
