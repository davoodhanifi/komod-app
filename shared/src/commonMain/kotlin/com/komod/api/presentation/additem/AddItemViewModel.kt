package com.komod.api.presentation.additem

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.komod.api.data.repository.AddItemRepository
import com.komod.api.platform.PickedImage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val MaxPhotosPerUpload = 5

class AddItemViewModel(
    private val addItemRepository: AddItemRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<AddItemUiState>(AddItemUiState.Initial)
    val uiState: StateFlow<AddItemUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<AddItemEffect>()
    val effects: SharedFlow<AddItemEffect> = _effects.asSharedFlow()

    fun onImagesSelected(images: List<PickedImage>) {
        if (images.isEmpty()) return

        if (images.size > MaxPhotosPerUpload) {
            viewModelScope.launch { _effects.emit(AddItemEffect.SelectionLimitExceeded) }
            return
        }

        uploadPhotos(images)
    }

    fun retryFailed() {
        val failedPhotos = (_uiState.value as? AddItemUiState.Error)?.failedPhotos ?: return
        uploadPhotos(failedPhotos)
    }

    fun reset() {
        _uiState.value = AddItemUiState.Initial
    }

    private fun uploadPhotos(photos: List<PickedImage>) {
        viewModelScope.launch {
            val total = photos.size
            val failed = mutableListOf<PickedImage>()
            var completedCount = 0

            fun emitProgress(currentFraction: Float) {
                _uiState.value = AddItemUiState.Uploading(
                    total = total,
                    completed = completedCount,
                    progress = ((completedCount + currentFraction) / total).coerceIn(0f, 1f),
                )
            }

            emitProgress(0f)

            for (photo in photos) {
                val uploaded = runCatching {
                    val imageInfo = addItemRepository.createImage()

                    // Success is determined by the storage upload alone — analysis runs
                    // asynchronously on the backend and must not block or fail this upload.
                    addItemRepository.uploadImage(imageInfo, photo.bytes, photo.mimeType) { fraction ->
                        emitProgress(fraction.coerceIn(0f, 1f))
                    }

                    // Persist locally so the Upload Queue can show this image right away.
                    addItemRepository.saveUploadedImage(imageInfo)

                    // Fire-and-forget on a scope that outlives this screen: uploading
                    // navigates back immediately, which would otherwise cancel this
                    // request mid-flight if it were tied to viewModelScope.
                    addItemRepository.triggerAnalysisInBackground(imageInfo.imageId)
                }.isSuccess

                if (!uploaded) failed += photo
                completedCount += 1
                emitProgress(0f)
            }

            if (failed.isEmpty()) {
                _uiState.value = AddItemUiState.Initial
                _effects.emit(AddItemEffect.UploadsSucceeded(count = total))
            } else {
                _uiState.value = AddItemUiState.Error(
                    message = "Some photos couldn't be uploaded. Please try again.",
                    failedPhotos = failed,
                )
            }
        }
    }
}
