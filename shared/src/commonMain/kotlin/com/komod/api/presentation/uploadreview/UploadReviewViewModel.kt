package com.komod.api.presentation.uploadreview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.komod.api.data.repository.UploadReviewRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UploadReviewViewModel(
    private val imageId: String,
    private val repository: UploadReviewRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UploadReviewUiState>(UploadReviewUiState.Loading)
    val uiState: StateFlow<UploadReviewUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = UploadReviewUiState.Loading
            runCatching { repository.getUploadedImageDetail(imageId) }
                .onSuccess { detail ->
                    _uiState.value = UploadReviewUiState.Ready(
                        detail = detail,
                        selectedItemIds = detail.items.map { it.id }.toSet(),
                    )
                }
                .onFailure { error ->
                    _uiState.value = UploadReviewUiState.Error(
                        message = error.message ?: "Something went wrong. Please try again.",
                    )
                }
        }
    }

    fun toggleItemSelection(itemId: String) {
        val current = _uiState.value as? UploadReviewUiState.Ready ?: return
        val updatedSelection = if (itemId in current.selectedItemIds) {
            current.selectedItemIds - itemId
        } else {
            current.selectedItemIds + itemId
        }
        _uiState.value = current.copy(selectedItemIds = updatedSelection)
    }
}
