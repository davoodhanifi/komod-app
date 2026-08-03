package com.komod.api.presentation.uploadreview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.komod.api.data.api.model.WardrobeItemReviewAction
import com.komod.api.data.api.model.WardrobeItemReviewRequestItem
import com.komod.api.data.repository.AddItemRepository
import com.komod.api.data.repository.AuthRepository
import com.komod.api.data.repository.UploadReviewConflictException
import com.komod.api.data.repository.UploadReviewForbiddenException
import com.komod.api.data.repository.UploadReviewNotFoundException
import com.komod.api.data.repository.UploadReviewRepository
import com.komod.api.data.repository.UploadReviewUnauthorizedException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UploadReviewViewModel(
    private val imageId: String,
    private val repository: UploadReviewRepository,
    private val authRepository: AuthRepository,
    private val addItemRepository: AddItemRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UploadReviewUiState>(UploadReviewUiState.Loading)
    val uiState: StateFlow<UploadReviewUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<UploadReviewEffect>()
    val effects: SharedFlow<UploadReviewEffect> = _effects.asSharedFlow()

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
        if (current.isSubmitting) return
        val updatedSelection = if (itemId in current.selectedItemIds) {
            current.selectedItemIds - itemId
        } else {
            current.selectedItemIds + itemId
        }
        _uiState.value = current.copy(selectedItemIds = updatedSelection)
    }

    fun submitReview() {
        val current = _uiState.value as? UploadReviewUiState.Ready ?: return
        if (current.isSubmitting) return

        val approvedCount = current.selectedItemIds.size
        val items = current.detail.items.map { item ->
            WardrobeItemReviewRequestItem(
                id = item.id,
                action = if (item.id in current.selectedItemIds) {
                    WardrobeItemReviewAction.Approve
                } else {
                    WardrobeItemReviewAction.Delete
                },
            )
        }

        viewModelScope.launch {
            _uiState.value = current.copy(isSubmitting = true)
            runCatching { repository.submitReview(items) }
                .onSuccess {
                    // The image is now fully reviewed and won't appear in the backend's
                    // "awaiting review" list anymore, so drop it from Recent Uploads now
                    // rather than waiting on a refetch that may never exclude it locally.
                    addItemRepository.removeUploadedImage(imageId)
                    _effects.emit(UploadReviewEffect.ReviewSucceeded(approvedCount))
                }
                .onFailure { error ->
                    when (error) {
                        is UploadReviewUnauthorizedException -> authRepository.signOut()
                        is UploadReviewConflictException -> {
                            addItemRepository.removeUploadedImage(imageId)
                            _effects.emit(UploadReviewEffect.ReviewConflict)
                        }
                        is UploadReviewForbiddenException -> {
                            _uiState.value = current.copy(isSubmitting = false)
                            _effects.emit(UploadReviewEffect.ReviewFailed("You don't have permission to do this."))
                        }
                        is UploadReviewNotFoundException -> {
                            _uiState.value = current.copy(isSubmitting = false)
                            _effects.emit(UploadReviewEffect.ReviewFailed("This upload could not be found."))
                        }
                        else -> {
                            _uiState.value = current.copy(isSubmitting = false)
                            _effects.emit(UploadReviewEffect.ReviewFailed("Something went wrong. Please try again."))
                        }
                    }
                }
        }
    }
}
