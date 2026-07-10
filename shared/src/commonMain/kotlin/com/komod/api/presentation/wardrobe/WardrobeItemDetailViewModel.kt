package com.komod.api.presentation.wardrobe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.komod.api.data.repository.WardrobeItemDeleteBadRequestException
import com.komod.api.data.repository.WardrobeItemDeleteNetworkException
import com.komod.api.data.repository.WardrobeItemDeleteNotFoundException
import com.komod.api.data.repository.WardrobeItemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class WardrobeItemDetailViewModel(
    private val itemId: String,
    private val repository: WardrobeItemRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<WardrobeItemDetailUiState>(WardrobeItemDetailUiState.Loading)
    val uiState: StateFlow<WardrobeItemDetailUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<WardrobeItemDetailEffect>()
    val effects: SharedFlow<WardrobeItemDetailEffect> = _effects.asSharedFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = WardrobeItemDetailUiState.Loading
            fetch()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            fetch()
            _isRefreshing.value = false
        }
    }

    fun onEvent(event: WardrobeItemDetailEvent) {
        when (event) {
            WardrobeItemDetailEvent.DeleteClicked -> showDeleteDialog()
            WardrobeItemDetailEvent.DeleteDismissed -> dismissDeleteDialog()
            WardrobeItemDetailEvent.DeleteConfirmed -> deleteItem()
        }
    }

    private suspend fun fetch() {
        runCatching { repository.getWardrobeItem(itemId) }
            .onSuccess { item -> _uiState.value = WardrobeItemDetailUiState.Success(item) }
            .onFailure { error ->
                _uiState.value = WardrobeItemDetailUiState.Error(
                    message = error.message ?: "Something went wrong. Please try again.",
                )
            }
    }

    private fun showDeleteDialog() {
        val state = _uiState.value
        if (state is WardrobeItemDetailUiState.Success) {
            _uiState.value = state.copy(isDeleteDialogVisible = true, isDeleting = false)
        }
    }

    private fun dismissDeleteDialog() {
        val state = _uiState.value
        if (state is WardrobeItemDetailUiState.Success) {
            _uiState.value = state.copy(isDeleteDialogVisible = false, isDeleting = false)
        }
    }

    private fun deleteItem() {
        val state = _uiState.value
        if (state !is WardrobeItemDetailUiState.Success || state.isDeleting) return

        _uiState.value = state.copy(isDeleteDialogVisible = true, isDeleting = true)

        viewModelScope.launch {
            try {
                repository.deleteWardrobeItem(itemId)
                _uiState.value = state.copy(isDeleteDialogVisible = false, isDeleting = false)
                _effects.emit(WardrobeItemDetailEffect.DeleteSucceeded)
            } catch (error: WardrobeItemDeleteNotFoundException) {
                _uiState.value = state.copy(isDeleteDialogVisible = false, isDeleting = false)
                _effects.emit(WardrobeItemDetailEffect.ItemMissing)
            } catch (error: WardrobeItemDeleteBadRequestException) {
                _uiState.value = state.copy(isDeleteDialogVisible = false, isDeleting = false)
                _effects.emit(
                    WardrobeItemDetailEffect.DeleteFailed(
                        message = "Couldn't delete the item.\nPlease try again.",
                    ),
                )
            } catch (error: WardrobeItemDeleteNetworkException) {
                _uiState.value = state.copy(isDeleteDialogVisible = false, isDeleting = false)
                _effects.emit(
                    WardrobeItemDetailEffect.DeleteFailed(
                        message = "Something went wrong.\nPlease try again.",
                    ),
                )
            }
        }
    }
}
