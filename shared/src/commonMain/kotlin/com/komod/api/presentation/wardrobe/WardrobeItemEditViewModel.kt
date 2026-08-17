package com.komod.api.presentation.wardrobe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.komod.api.core.error.ErrorMapper
import com.komod.api.data.api.model.WardrobeItemStatus
import com.komod.api.data.repository.WardrobeItemRepository
import com.komod.api.data.repository.WardrobeItemUpdateBadRequestException
import com.komod.api.data.repository.WardrobeItemUpdateNetworkException
import com.komod.api.data.repository.WardrobeItemUpdateNotFoundException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WardrobeItemEditViewModel(
    private val itemId: String,
    private val repository: WardrobeItemRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<WardrobeItemEditUiState>(WardrobeItemEditUiState.Loading)
    val uiState: StateFlow<WardrobeItemEditUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<WardrobeItemEditEffect>()
    val effects: SharedFlow<WardrobeItemEditEffect> = _effects.asSharedFlow()

    init {
        loadItem()
    }

    fun loadItem() {
        viewModelScope.launch {
            _uiState.value = WardrobeItemEditUiState.Loading
            runCatching { repository.getWardrobeItem(itemId) }
                .onSuccess { item ->
                    _uiState.value = WardrobeItemEditUiState.Ready(item.toEditContent())
                }
                .onFailure { error ->
                    _uiState.value = WardrobeItemEditUiState.Error(
                        message = ErrorMapper.toUserMessage(error, tag = "WardrobeItemEditViewModel"),
                    )
                }
        }
    }

    fun updateCategory(value: String) = updateForm { copy(category = value) }
    fun updateSubcategory(value: String) = updateForm { copy(subcategory = value) }
    fun updateName(value: String) = updateForm { copy(name = value) }
    fun updateBrand(value: String) = updateForm { copy(brand = value) }
    fun updateMaterial(value: String) = updateForm { copy(material = value) }
    fun updatePrimaryColor(value: String) = updateForm { copy(primaryColor = value) }
    fun updateFormality(value: String) = updateForm { copy(formality = value) }
    fun toggleSeason(value: String) = updateForm {
        copy(seasons = if (seasons.contains(value)) seasons - value else seasons + value)
    }
    fun toggleOccasion(value: String) = updateForm {
        copy(occasions = if (occasions.contains(value)) occasions - value else occasions + value)
    }
    fun updateStatus(value: WardrobeItemStatus) = updateForm { copy(status = value) }

    fun saveChanges() {
        val state = _uiState.value as? WardrobeItemEditUiState.Ready ?: return
        if (state.content.isSaving) return

        val validationError = state.content.form.validate()
        if (validationError != null) {
            viewModelScope.launch {
                _effects.emit(WardrobeItemEditEffect.SaveFailed(validationError))
            }
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(content = state.content.copy(isSaving = true))
            try {
                repository.updateWardrobeItem(
                    id = itemId,
                    request = state.content.form.toUpdateRequest(),
                )
                _uiState.value = state.copy(content = state.content.copy(isSaving = false))
                _effects.emit(WardrobeItemEditEffect.Saved)
            } catch (error: WardrobeItemUpdateNotFoundException) {
                _uiState.value = state.copy(content = state.content.copy(isSaving = false))
                _effects.emit(WardrobeItemEditEffect.ItemNotFound)
            } catch (error: WardrobeItemUpdateBadRequestException) {
                _uiState.value = state.copy(content = state.content.copy(isSaving = false))
                _effects.emit(
                    WardrobeItemEditEffect.SaveFailed(ErrorMapper.toUserMessage(error, tag = "WardrobeItemEditViewModel")),
                )
            } catch (error: WardrobeItemUpdateNetworkException) {
                _uiState.value = state.copy(content = state.content.copy(isSaving = false))
                _effects.emit(
                    WardrobeItemEditEffect.SaveFailed(ErrorMapper.toUserMessage(error, tag = "WardrobeItemEditViewModel")),
                )
            }
        }
    }

    private fun updateForm(transform: WardrobeItemEditFormState.() -> WardrobeItemEditFormState) {
        val state = _uiState.value as? WardrobeItemEditUiState.Ready ?: return
        if (state.content.isSaving) return
        _uiState.value = state.copy(content = state.content.copy(form = state.content.form.transform()))
    }
}
