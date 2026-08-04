package com.komod.api.presentation.cropeditor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.komod.api.core.error.ErrorMapper
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
                // Items in the review flow are still pending approval, so they're only
                // reachable through the upload's own item list — GET wardrobe-items/{id}
                // doesn't recognize them yet.
                val detail = uploadReviewRepository.getUploadedImageDetail(imageId)
                val item = detail.items.find { it.id == wardrobeItemId }
                    ?: throw UploadedItemNotFoundException()
                val itemTitle = item.itemName ?: item.category.toWardrobeLabel()
                Triple(itemTitle, item.boundingBox, detail.originalImageUrl)
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
                _uiState.value = current.copy(isSaving = false)
                _effects.emit(
                    CropEditorEffect.SaveFailed(ErrorMapper.toUserMessage(error, tag = "CropEditorViewModel")),
                )
            } catch (error: WardrobeItemUpdateBadRequestException) {
                _uiState.value = current.copy(isSaving = false)
                _effects.emit(
                    CropEditorEffect.SaveFailed(ErrorMapper.toUserMessage(error, tag = "CropEditorViewModel")),
                )
            } catch (error: WardrobeItemUpdateNetworkException) {
                _uiState.value = current.copy(isSaving = false)
                _effects.emit(
                    CropEditorEffect.SaveFailed(ErrorMapper.toUserMessage(error, tag = "CropEditorViewModel")),
                )
            }
        }
    }
}
