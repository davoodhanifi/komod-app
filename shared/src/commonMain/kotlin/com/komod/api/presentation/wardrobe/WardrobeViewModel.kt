package com.komod.api.presentation.wardrobe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.komod.api.data.repository.WardrobeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WardrobeViewModel(
    private val wardrobeRepository: WardrobeRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<WardrobeUiState>(WardrobeUiState.Loading)
    val uiState: StateFlow<WardrobeUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadItems()
    }

    fun loadItems() {
        viewModelScope.launch {
            _uiState.value = WardrobeUiState.Loading
            fetchItems()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            fetchItems()
            _isRefreshing.value = false
        }
    }

    private suspend fun fetchItems() {
        runCatching { wardrobeRepository.getWardrobeItems() }
            .onSuccess { items -> _uiState.value = WardrobeUiState.Success(items) }
            .onFailure { error ->
                _uiState.value = WardrobeUiState.Error(
                    message = error.message ?: "Something went wrong. Please try again.",
                )
            }
    }
}
