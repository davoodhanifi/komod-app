package com.komod.api.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.komod.api.data.repository.HomeRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val homeRepository: HomeRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    summaryState = WardrobeSummaryState.Loading,
                    recentItemsState = RecentItemsState.Loading,
                )
            }

            // Load both concurrently
            val summaryDeferred = async { fetchSummary() }
            val recentItemsDeferred = async { fetchRecentItems() }

            summaryDeferred.await()
            recentItemsDeferred.await()
        }
    }

    fun selectOccasion(occasion: String) {
        _uiState.update { it.copy(selectedOccasion = occasion) }
    }

    fun retryLoadSummary() {
        viewModelScope.launch {
            _uiState.update { it.copy(summaryState = WardrobeSummaryState.Loading) }
            fetchSummary()
        }
    }

    fun retryLoadRecentItems() {
        viewModelScope.launch {
            _uiState.update { it.copy(recentItemsState = RecentItemsState.Loading) }
            fetchRecentItems()
        }
    }

    private suspend fun fetchSummary() {
        runCatching { homeRepository.getWardrobeSummary() }
            .onSuccess { summary ->
                _uiState.update {
                    it.copy(summaryState = WardrobeSummaryState.Success(summary))
                }
            }
            .onFailure { error ->
                _uiState.update {
                    it.copy(
                        summaryState = WardrobeSummaryState.Error(
                            message = error.message ?: "Couldn't load wardrobe summary.",
                        ),
                    )
                }
            }
    }

    private suspend fun fetchRecentItems() {
        runCatching { homeRepository.getRecentItems() }
            .onSuccess { items ->
                _uiState.update {
                    it.copy(recentItemsState = RecentItemsState.Success(items))
                }
            }
            .onFailure { error ->
                _uiState.update {
                    it.copy(
                        recentItemsState = RecentItemsState.Error(
                            message = error.message ?: "Couldn't load recent items.",
                        ),
                    )
                }
            }
    }
}
