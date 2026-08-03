package com.komod.api.presentation.uploadreview

sealed interface UploadReviewEffect {
    data class ReviewSucceeded(val approvedCount: Int) : UploadReviewEffect

    data object ReviewConflict : UploadReviewEffect

    data class ReviewFailed(val message: String) : UploadReviewEffect
}
