package com.komod.api.presentation.cropeditor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.komod.api.core.error.AppLogger
import com.komod.api.core.error.ErrorMapper
import com.komod.api.core.error.UserFacingMessages
import com.komod.api.data.repository.UploadReviewRepository
import com.komod.api.data.repository.UploadedItemNotFoundException
import com.komod.api.data.repository.WardrobeItemRepository
import com.komod.api.data.repository.WardrobeItemUpdateBadRequestException
import com.komod.api.data.repository.WardrobeItemUpdateNetworkException
import com.komod.api.data.repository.WardrobeItemUpdateNotFoundException
import com.komod.api.domain.model.BoundingBox
import com.komod.api.domain.model.CropEditorState
import com.komod.api.presentation.wardrobe.toWardrobeLabel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CropEditorViewModel(
    private val imageId: String,
    private val wardrobeItemId: String,
    private val uploadReviewRepository: UploadReviewRepository,
    private val wardrobeItemRepository: WardrobeItemRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<CropEditorUiState>(CropEditorUiState.Loading)
    val uiState: StateFlow<CropEditorUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<CropEditorEffect>()
    val effects: SharedFlow<CropEditorEffect> = _effects.asSharedFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = CropEditorUiState.Loading
            runCatching {
                // Already-approved items live in the wardrobe and are reachable via
                // GET wardrobe-items/{id}. Items still awaiting review aren't recognized
                // by that endpoint yet, so fall back to the upload's own item list, which
                // is the only place they're reachable from until they're approved.
                loadApprovedWardrobeItem() ?: loadPendingReviewItem()
            }.onSuccess { (itemTitle, boundingBox, originalImageUrl) ->
                _uiState.value = CropEditorUiState.Ready(
                    itemTitle = itemTitle,
                    originalImageUrl = originalImageUrl,
                    cropState = CropEditorState(
                        originalBoundingBox = boundingBox,
                        currentBoundingBox = boundingBox,
                    ),
                )
            }.onFailure { error ->
                _uiState.value = CropEditorUiState.Error(
                    message = ErrorMapper.toUserMessage(error, tag = "CropEditorViewModel"),
                )
            }
        }
    }

    private suspend fun loadApprovedWardrobeItem(): Triple<String, BoundingBox, String?>? {
        return runCatching {
            val item = wardrobeItemRepository.getWardrobeItem(wardrobeItemId)
            val itemTitle = item.itemName ?: item.category.toWardrobeLabel()
            // Prefer the storage path the backend now returns directly on the item — it's
            // a single signed-URL call. Only fall back to the old imageId-based lookup
            // (GET images/{imageId}, which 404s for a lot of items on this backend) for
            // items fetched before that field existed.
            val originalImageUrl = item.originalImageStoragePath
                ?.takeIf { it.isNotBlank() }
                ?.let { path -> wardrobeItemRepository.createSignedUrl(path) }
                ?: wardrobeItemRepository.getOriginalImageUrl(item.imageId)
            Triple(itemTitle, item.boundingBox, originalImageUrl)
        }.getOrNull()
    }

    private suspend fun loadPendingReviewItem(): Triple<String, BoundingBox, String?> {
        val detail = uploadReviewRepository.getUploadedImageDetail(imageId)
        val item = detail.items.find { it.id == wardrobeItemId }
            ?: throw UploadedItemNotFoundException()
        val itemTitle = item.itemName ?: item.category.toWardrobeLabel()
        return Triple(itemTitle, item.boundingBox, detail.originalImageUrl)
    }

    fun updateBoundingBox(boundingBox: BoundingBox) {
        val current = _uiState.value as? CropEditorUiState.Ready ?: return
        if (current.isSaving) return
        _uiState.value = current.copy(cropState = current.cropState.withBoundingBox(boundingBox))
    }

    fun resetCrop() {
        val current = _uiState.value as? CropEditorUiState.Ready ?: return
        if (current.isSaving) return
        _uiState.value = current.copy(cropState = current.cropState.reset())
    }

    fun saveCrop() {
        val current = _uiState.value as? CropEditorUiState.Ready ?: return
        if (current.isSaving) return

        viewModelScope.launch {
            _uiState.value = current.copy(isSaving = true)
            try {
                wardrobeItemRepository.updateWardrobeItemBoundingBox(
                    id = wardrobeItemId,
                    boundingBox = current.cropState.currentBoundingBox,
                )
                _effects.emit(CropEditorEffect.CropSaved)
            } catch (error: WardrobeItemUpdateNotFoundException) {
                onSaveFailed(current, error)
            } catch (error: WardrobeItemUpdateBadRequestException) {
                onSaveFailed(current, error)
            } catch (error: WardrobeItemUpdateNetworkException) {
                onSaveFailed(current, error)
            }
        }
    }

    // Any save failure just shows a generic message for now — no per-status-code
    // messaging until that's actually needed.
    private suspend fun onSaveFailed(current: CropEditorUiState.Ready, error: Throwable) {
        AppLogger.e("CropEditorViewModel", "Failed to save crop (${error::class.simpleName})", error)
        _uiState.value = current.copy(isSaving = false)
        _effects.emit(CropEditorEffect.SaveFailed(UserFacingMessages.Generic))
    }
}
