package com.komod.api.presentation.uploadreview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.komod.api.core.error.ErrorMapper
import com.komod.api.core.error.PlanLimitExceededException
import com.komod.api.data.api.model.WardrobeItemReviewAction
import com.komod.api.data.api.model.WardrobeItemReviewRequestItem
import com.komod.api.data.repository.AddItemRepository
import com.komod.api.data.repository.AuthRepository
import com.komod.api.data.repository.UploadReviewConflictException
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
                        message = ErrorMapper.toUserMessage(error, tag = "UploadReviewViewModel"),
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

    fun toggleSelectAll() {
        val current = _uiState.value as? UploadReviewUiState.Ready ?: return
        if (current.isSubmitting) return
        val allIds = current.detail.items.map { it.id }.toSet()
        val updatedSelection = if (allIds.isNotEmpty() && current.selectedItemIds.containsAll(allIds)) {
            emptySet()
        } else {
            allIds
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

        submitItems(current, items) { UploadReviewEffect.ReviewSucceeded(approvedCount) }
    }

    fun deleteUpload() {
        val current = _uiState.value as? UploadReviewUiState.Ready ?: return
        if (current.isSubmitting) return

        // Marking every item Delete, regardless of the current selection, is how
        // the review endpoint expresses "discard this whole upload" — there's no
        // separate delete-image endpoint.
        val items = current.detail.items.map { item ->
            WardrobeItemReviewRequestItem(id = item.id, action = WardrobeItemReviewAction.Delete)
        }

        submitItems(current, items) { UploadReviewEffect.UploadDeleted }
    }

    private fun submitItems(
        current: UploadReviewUiState.Ready,
        items: List<WardrobeItemReviewRequestItem>,
        onSuccessEffect: () -> UploadReviewEffect,
    ) {
        viewModelScope.launch {
            _uiState.value = current.copy(isSubmitting = true)
            runCatching { repository.submitReview(items) }
                .onSuccess {
                    // The image is now fully reviewed and won't appear in the backend's
                    // "awaiting review" list anymore, so drop it from Recent Uploads now
                    // rather than waiting on a refetch that may never exclude it locally.
                    addItemRepository.removeUploadedImage(imageId)
                    _effects.emit(onSuccessEffect())
                }
                .onFailure { error ->
                    when (error) {
                        is UploadReviewUnauthorizedException -> {
                            val message = ErrorMapper.toUserMessage(error, tag = "UploadReviewViewModel")
                            _effects.emit(UploadReviewEffect.ReviewFailed(message))
                            authRepository.signOut()
                        }
                        is UploadReviewConflictException -> {
                            addItemRepository.removeUploadedImage(imageId)
                            _effects.emit(UploadReviewEffect.ReviewConflict)
                        }
                        is PlanLimitExceededException -> {
                            // Do not drop or auto-reject any items — leave the user's
                            // selection exactly as it was so they can adjust it and retry
                            // once they've freed up capacity.
                            _uiState.value = current.copy(isSubmitting = false)
                            _effects.emit(UploadReviewEffect.PlanLimitReached)
                        }
                        else -> {
                            _uiState.value = current.copy(isSubmitting = false)
                            val message = ErrorMapper.toUserMessage(error, tag = "UploadReviewViewModel")
                            _effects.emit(UploadReviewEffect.ReviewFailed(message))
                        }
                    }
                }
        }
    }
}
