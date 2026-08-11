package com.komod.api.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.komod.api.core.error.ErrorMapper
import com.komod.api.data.location.WeatherLocationResult
import com.komod.api.data.location.WeatherLocationService
import com.komod.api.data.preferences.CachedOutfitOfTheDay
import com.komod.api.data.preferences.OutfitOfTheDayCache
import com.komod.api.data.preferences.get
import com.komod.api.data.preferences.save
import com.komod.api.data.repository.HomeRepository
import com.komod.api.data.repository.OutfitRepository
import com.komod.api.data.repository.WeatherRepository
import com.komod.api.domain.model.OutfitItem
import com.komod.api.domain.model.OutfitOccasion
import com.komod.api.domain.model.WardrobeSummary
import kotlin.time.Clock
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val homeRepository: HomeRepository,
    private val weatherLocationService: WeatherLocationService,
    private val weatherRepository: WeatherRepository,
    private val outfitRepository: OutfitRepository,
    private val outfitOfTheDayCache: OutfitOfTheDayCache,
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

        val today = todayDateString()
        val cached = outfitOfTheDayCache.get()
        if (cached != null && cached.date == today) {
            val cachedState = runCatching {
                outfitRepository.hydrateOutfitItems(cached.wardrobeItemIds)
            }.getOrNull()?.let { items -> cached.toAvailableState(items) }

            if (cachedState != null) {
                _uiState.update { it.copy(outfitOfTheDayState = cachedState) }
                return
            }
        }

        _uiState.update { it.copy(outfitOfTheDayState = OutfitOfTheDayState.Loading) }

        val locationResult = runCatching { weatherLocationService.getCurrentLocation() }.getOrNull()
        val locationPermissionDenied = locationResult is WeatherLocationResult.PermissionRequired

        // Kept as the real domain object (not just the display fields) so the actual
        // fetched weather — not a reconstructed approximation — is what generateOutfits()
        // receives.
        val weatherCurrent = if (locationResult is WeatherLocationResult.Success) {
            runCatching {
                weatherRepository.getCurrentWeather(locationResult.latitude, locationResult.longitude)
            }.getOrNull()
        } else {
            null
        }

        val outfit = runCatching {
            outfitRepository.generateOutfits(
                occasion = OutfitOccasion.Outdoor.apiValue,
                weather = weatherCurrent,
            )
        }.getOrNull()?.firstOrNull()

        if (outfit == null) {
            _uiState.update { it.copy(outfitOfTheDayState = null) }
            return
        }

        val weather = weatherCurrent?.let {
            OutfitOfTheDayWeather(
                temperatureC = it.temperatureC,
                condition = it.condition,
                weatherCode = it.weatherCode,
                isRaining = it.isRaining,
                isSnowing = it.isSnowing,
            )
        }

        outfitOfTheDayCache.save(
            CachedOutfitOfTheDay(
                date = today,
                occasion = OutfitOccasion.Outdoor.apiValue,
                id = outfit.id,
                name = outfit.name,
                reason = outfit.reason,
                matchScore = outfit.matchScore,
                wardrobeItemIds = outfit.wardrobeItemIds,
                temperatureC = weather?.temperatureC,
                condition = weather?.condition,
                weatherCode = weather?.weatherCode,
                isRaining = weather?.isRaining ?: false,
                isSnowing = weather?.isSnowing ?: false,
                locationPermissionDenied = locationPermissionDenied,
            ),
        )

        _uiState.update {
            it.copy(
                outfitOfTheDayState = OutfitOfTheDayState.Available(
                    id = outfit.id,
                    occasionLabel = OutfitOccasion.Outdoor.label,
                    name = outfit.name,
                    reason = outfit.reason,
                    matchScore = outfit.matchScore,
                    wardrobeItemIds = outfit.wardrobeItemIds,
                    items = outfit.items,
                    weather = weather,
                    locationPermissionDenied = locationPermissionDenied,
                ),
            )
        }
    }

    private fun CachedOutfitOfTheDay.toAvailableState(items: List<OutfitItem>): OutfitOfTheDayState.Available {
        val cachedWeather = if (temperatureC != null && condition != null && weatherCode != null) {
            OutfitOfTheDayWeather(
                temperatureC = temperatureC,
                condition = condition,
                weatherCode = weatherCode,
                isRaining = isRaining,
                isSnowing = isSnowing,
            )
        } else {
            null
        }

        return OutfitOfTheDayState.Available(
            id = id,
            occasionLabel = OutfitOccasion.Outdoor.label,
            name = name,
            reason = reason,
            matchScore = matchScore,
            wardrobeItemIds = wardrobeItemIds,
            items = items,
            weather = cachedWeather,
            locationPermissionDenied = locationPermissionDenied,
        )
    }

    private fun todayDateString(): String = Clock.System.now().toString().substringBefore("T")

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
