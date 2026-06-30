package com.komod.api.presentation.additem

sealed interface AddItemUiState {
    data object Initial : AddItemUiState

    data class Uploading(
        val imageThumbnail: ByteArray,
        val storagePath: String,
        val overallProgress: Float,
        val steps: List<UploadStep>,
    ) : AddItemUiState

    data object Success : AddItemUiState

    data class Error(
        val message: String,
        val lastImageBytes: ByteArray? = null,
        val lastMimeType: String = "image/jpeg",
    ) : AddItemUiState
}

data class UploadStep(
    val label: String,
    val status: StepStatus,
)

enum class StepStatus {
    Pending,
    InProgress,
    Completed,
}
