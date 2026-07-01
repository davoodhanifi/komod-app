package com.komod.api.presentation.wardrobe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.komod.api.data.repository.WardrobeItemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WardrobeItemDetailViewModel(
    private val itemId: String,
    private val repository: WardrobeItemRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<WardrobeItemDetailUiState>(WardrobeItemDetailUiState.Loading)
    val uiState: StateFlow<WardrobeItemDetailUiState> = _uiState.asStateFlow()

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

    private suspend fun fetch() {
        runCatching { repository.getWardrobeItem(itemId) }
            .onSuccess { item -> _uiState.value = WardrobeItemDetailUiState.Success(item) }
            .onFailure { error ->
                _uiState.value = WardrobeItemDetailUiState.Error(
                    message = error.message ?: "Something went wrong. Please try again.",
                )
            }
    }
}
