package com.komod.api.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.komod.api.core.error.ErrorMapper
import com.komod.api.data.location.WeatherLocationResult
import com.komod.api.data.location.WeatherLocationService
import com.komod.api.data.repository.HomeRepository
import com.komod.api.data.repository.OutfitRepository
import com.komod.api.domain.model.WardrobeSummary
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val homeRepository: HomeRepository,
    private val weatherLocationService: WeatherLocationService,
    private val outfitRepository: OutfitRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    summaryState = WardrobeSummaryState.Loading,
                    recentItemsState = RecentItemsState.Loading,
                    savedOutfitsState = SavedOutfitsState.Loading,
                    outfitOfTheDayState = null,
                )
            }

            // Load concurrently
            val summaryDeferred = async { fetchSummary() }
            val recentItemsDeferred = async { fetchRecentItems() }
            val savedOutfitsDeferred = async { fetchSavedOutfits() }

            summaryDeferred.await()
            recentItemsDeferred.await()
            savedOutfitsDeferred.await()
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
                loadOutfitOfTheDay(summary)
            }
            .onFailure { error ->
                _uiState.update {
                    it.copy(
                        summaryState = WardrobeSummaryState.Error(
                            message = ErrorMapper.toUserMessage(error, tag = "HomeViewModel"),
                        ),
                    )
                }
            }
    }

    private suspend fun loadOutfitOfTheDay(summary: WardrobeSummary) {
        if (!hasEnoughItemsForOutfit(summary)) {
            _uiState.update { it.copy(outfitOfTheDayState = OutfitOfTheDayState.NotEnoughItems) }
            return
        }

        // The backend is the source of truth for the current Outfit of the Day and its
        // 6-hour validity window — every load asks it fresh rather than relying on any
        // local cache, so a new window's outfit shows up without needing a mobile-side
        // timer.
        _uiState.update { it.copy(outfitOfTheDayState = OutfitOfTheDayState.Loading) }

        // GET /outfits/today needs coordinates on every call (even on a cache hit, to
        // resolve the local-time window) — without location there's nothing it can return.
        val locationResult = runCatching { weatherLocationService.getCurrentLocation() }.getOrNull()
        if (locationResult !is WeatherLocationResult.Success) {
            _uiState.update { it.copy(outfitOfTheDayState = null) }
            return
        }

        val outfitOfTheDay = runCatching {
            outfitRepository.getOutfitOfTheDay(
                latitude = locationResult.latitude,
                longitude = locationResult.longitude,
            )
        }.getOrNull()

        if (outfitOfTheDay == null || outfitOfTheDay.outfits.isEmpty()) {
            _uiState.update { it.copy(outfitOfTheDayState = null) }
            return
        }

        // This is the exact snapshot the outfits were generated against, not a live
        // reading — displayed as-is rather than re-fetched from /weather/current.
        val snapshot = outfitOfTheDay.weather
        val weather = OutfitOfTheDayWeather(
            temperatureC = snapshot.temperatureC,
            condition = snapshot.condition,
            isRaining = snapshot.isRaining,
            isSnowing = snapshot.isSnowing,
            next6Hours = if (snapshot.next6HourMinTemperatureC != null && snapshot.next6HourMaxTemperatureC != null) {
                OutfitOfTheDayWeatherRange(
                    minTemperatureC = snapshot.next6HourMinTemperatureC,
                    maxTemperatureC = snapshot.next6HourMaxTemperatureC,
                )
            } else {
                null
            },
        )

        _uiState.update {
            it.copy(
                outfitOfTheDayState = OutfitOfTheDayState.Available(
                    outfits = outfitOfTheDay.outfits,
                    weather = weather,
                ),
            )
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
                            message = ErrorMapper.toUserMessage(error, tag = "HomeViewModel"),
                        ),
                    )
                }
            }
    }

    private suspend fun fetchSavedOutfits() {
        runCatching { homeRepository.getSavedOutfits() }
            .onSuccess { outfits ->
                _uiState.update {
                    it.copy(savedOutfitsState = SavedOutfitsState.Success(outfits))
                }
            }
            .onFailure { error ->
                // Logged for diagnostics, but the message itself is discarded: a failed
                // saved-outfits load hides the section entirely rather than showing an error.
                ErrorMapper.toUserMessage(error, tag = "HomeViewModel")
                _uiState.update { it.copy(savedOutfitsState = SavedOutfitsState.Error) }
            }
    }
}
