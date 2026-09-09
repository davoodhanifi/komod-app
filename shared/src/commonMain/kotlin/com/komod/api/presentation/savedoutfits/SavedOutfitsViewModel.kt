package com.komod.api.presentation.savedoutfits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.komod.api.core.error.ErrorMapper
import com.komod.api.data.repository.HomeRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SavedOutfitsEffect {
    data class ShowMessage(val message: String) : SavedOutfitsEffect
}

// Backend caps pageSize at 10 for GET /outfits.
private const val SavedOutfitsPageSize = 10

class SavedOutfitsViewModel(
    private val homeRepository: HomeRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<SavedOutfitsUiState>(SavedOutfitsUiState.Loading)
    val uiState: StateFlow<SavedOutfitsUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _effects = MutableSharedFlow<SavedOutfitsEffect>()
    val effects: SharedFlow<SavedOutfitsEffect> = _effects.asSharedFlow()

    // Number of pages already fetched into the current Success.outfits list, and whether
    // the backend reported more pages beyond that. Reset on every full reload (load/refresh)
    // and advanced by one on each successful loadMoreOutfits().
    private var loadedPageCount = 0
    private var hasNextPage = false
    private var isLoadingNextPage = false

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = SavedOutfitsUiState.Loading
            fetchFirstPage()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            fetchFirstPage()
            _isRefreshing.value = false
        }
    }

    // Called by the screen as the grid scrolls near the end. No-op while a page is
    // already loading or the backend reported no further pages.
    fun loadMoreOutfits() {
        if (isLoadingNextPage || !hasNextPage) return
        val current = _uiState.value
        if (current !is SavedOutfitsUiState.Success) return

        isLoadingNextPage = true
        _uiState.value = current.copy(isLoadingMore = true)
        viewModelScope.launch {
            runCatching {
                homeRepository.getSavedOutfitsPage(pageNumber = loadedPageCount + 1, pageSize = SavedOutfitsPageSize)
            }
                .onSuccess { page ->
                    loadedPageCount += 1
                    hasNextPage = page.hasNextPage
                    val existing = (_uiState.value as? SavedOutfitsUiState.Success)?.outfits.orEmpty()
                    _uiState.value = SavedOutfitsUiState.Success(
                        outfits = existing + page.outfits,
                        isLoadingMore = false,
                        hasNextPage = page.hasNextPage,
                    )
                }
                .onFailure { error ->
                    (_uiState.value as? SavedOutfitsUiState.Success)?.let {
                        _uiState.value = it.copy(isLoadingMore = false)
                    }
                    _effects.emit(
                        SavedOutfitsEffect.ShowMessage(
                            ErrorMapper.toUserMessage(error, tag = "SavedOutfitsViewModel"),
                        ),
                    )
                }
            isLoadingNextPage = false
        }
    }

    private suspend fun fetchFirstPage() {
        runCatching {
            homeRepository.getSavedOutfitsPage(pageNumber = 1, pageSize = SavedOutfitsPageSize)
        }
            .onSuccess { page ->
                loadedPageCount = 1
                hasNextPage = page.hasNextPage
                _uiState.value = SavedOutfitsUiState.Success(outfits = page.outfits, hasNextPage = page.hasNextPage)
            }
            .onFailure { error ->
                loadedPageCount = 0
                hasNextPage = false
                _uiState.value = SavedOutfitsUiState.Error(
                    message = ErrorMapper.toUserMessage(error, tag = "SavedOutfitsViewModel"),
                )
            }
    }
}
