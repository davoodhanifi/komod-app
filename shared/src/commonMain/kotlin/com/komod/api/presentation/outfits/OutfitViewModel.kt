package com.komod.api.presentation.outfits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.komod.api.core.error.ErrorContext
import com.komod.api.core.error.ErrorMapper
import com.komod.api.data.repository.OutfitRepository
import com.komod.api.data.repository.WeatherRepository
import com.komod.api.data.location.WeatherLocationResult
import com.komod.api.data.location.WeatherLocationService
import com.komod.api.data.preferences.WeatherPreferences
import com.komod.api.domain.model.OutfitOccasion
import com.komod.api.domain.model.OutfitStyle
import com.komod.api.domain.model.Outfit
import com.komod.api.platform.AppSettingsOpener
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OutfitViewModel(
    private val outfitRepository: OutfitRepository,
    private val weatherRepository: WeatherRepository,
    private val weatherPreferences: WeatherPreferences,
    private val weatherLocationService: WeatherLocationService,
    private val appSettingsOpener: AppSettingsOpener,
) : ViewModel() {
    private val _uiState = MutableStateFlow(OutfitUiState())
    val uiState: StateFlow<OutfitUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<OutfitEffect>()
    val effects: SharedFlow<OutfitEffect> = _effects.asSharedFlow()

    init {
        if (weatherPreferences.isEnabled()) {
            _uiState.update { it.copy(weatherUiState = WeatherUiState.Loading) }
            refreshWeather()
        }
    }

    fun selectOccasion(occasion: OutfitOccasion) {
        _uiState.value = _uiState.value.copy(
            selectedOccasion = occasion,
            errorMessage = null,
        )
    }

    fun selectStyle(style: OutfitStyle?) {
        _uiState.value = _uiState.value.copy(
            selectedStyle = style,
            errorMessage = null,
        )
    }

    fun generateOutfits() {
        viewModelScope.launch {
            if (_uiState.value.isGenerating) return@launch

            _uiState.value = _uiState.value.copy(isGenerating = true, errorMessage = null)
            runCatching {
                val state = _uiState.value
                outfitRepository.generateOutfits(
                    occasion = state.selectedOccasion.apiValue,
                    style = state.selectedStyle?.apiValue,
                    weather = (state.weatherUiState as? WeatherUiState.Loaded)?.weather,
                )
            }.onSuccess { outfits ->
                _uiState.value = _uiState.value.copy(
                    outfits = outfits,
                    isGenerating = false,
                    errorMessage = null,
                )
            }.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    errorMessage = ErrorMapper.toUserMessage(throwable, tag = "OutfitViewModel"),
                )
            }
        }
    }

    // Lets other entry points (e.g. tapping a Saved Outfits card on Home) reuse the
    // Outfit Details route, which looks outfits up by id from this ViewModel's list.
    // Since the outfit already came from GET /v1/outfits, its id is itself the
    // server-assigned saved outfit id, so it's recorded as already-saved (self-mapped)
    // rather than eligible to be saved again.
    fun showSavedOutfitDetails(outfit: Outfit) {
        _uiState.update { state ->
            val outfits = if (state.outfits.any { it.id == outfit.id }) state.outfits else state.outfits + outfit
            state.copy(
                outfits = outfits,
                savedOutfitIds = state.savedOutfitIds + (outfit.id to outfit.id),
            )
        }
    }

    // For outfits that exist only client-side and were never persisted (e.g. Home's
    // Outfit of the Day) — unlike showSavedOutfitDetails, this does NOT mark the outfit
    // as already-saved, since its id is a locally-generated one, not a real saved-outfit id.
    fun showGeneratedOutfitDetails(outfit: Outfit) {
        _uiState.update { state ->
            val outfits = if (state.outfits.any { it.id == outfit.id }) state.outfits else state.outfits + outfit
            state.copy(outfits = outfits)
        }
    }

    fun toggleSaveOutfit(outfit: Outfit) {
        if (outfit.id in _uiState.value.savedOutfitIds) {
            unsaveOutfit(outfit)
        } else {
            saveOutfit(outfit)
        }
    }

    private fun saveOutfit(outfit: Outfit) {
        val state = _uiState.value
        if (outfit.id in state.savingOutfitIds || outfit.id in state.unsavingOutfitIds || outfit.id in state.savedOutfitIds) return

        _uiState.update { it.copy(savingOutfitIds = it.savingOutfitIds + outfit.id) }
        viewModelScope.launch {
            runCatching {
                outfitRepository.saveOutfit(outfit)
            }.onSuccess { savedOutfitId ->
                _uiState.update {
                    it.copy(
                        savingOutfitIds = it.savingOutfitIds - outfit.id,
                        savedOutfitIds = it.savedOutfitIds + (outfit.id to savedOutfitId),
                    )
                }
            }.onFailure { throwable ->
                _uiState.update { it.copy(savingOutfitIds = it.savingOutfitIds - outfit.id) }
                _effects.emit(
                    OutfitEffect.ShowSnackbar(
                        ErrorMapper.toUserMessage(throwable, tag = "OutfitViewModel"),
                    ),
                )
            }
        }
    }

    private fun unsaveOutfit(outfit: Outfit) {
        val state = _uiState.value
        val savedOutfitId = state.savedOutfitIds[outfit.id] ?: return
        if (outfit.id in state.savingOutfitIds || outfit.id in state.unsavingOutfitIds) return

        _uiState.update { it.copy(unsavingOutfitIds = it.unsavingOutfitIds + outfit.id) }
        viewModelScope.launch {
            runCatching {
                outfitRepository.unsaveOutfit(savedOutfitId)
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        unsavingOutfitIds = it.unsavingOutfitIds - outfit.id,
                        savedOutfitIds = it.savedOutfitIds - outfit.id,
                    )
                }
            }.onFailure { throwable ->
                _uiState.update { it.copy(unsavingOutfitIds = it.unsavingOutfitIds - outfit.id) }
                _effects.emit(
                    OutfitEffect.ShowSnackbar(
                        ErrorMapper.toUserMessage(throwable, tag = "OutfitViewModel"),
                    ),
                )
            }
        }
    }

    fun setWeatherEnabled(enabled: Boolean) {
        weatherPreferences.setEnabled(enabled)
        if (!enabled) {
            _uiState.value = _uiState.value.copy(weatherUiState = WeatherUiState.WeatherDisabled)
            return
        }
        _uiState.value = _uiState.value.copy(weatherUiState = WeatherUiState.Loading)
        refreshWeather()
    }

    fun markWeatherPermissionRequired() {
        weatherPreferences.setEnabled(false)
        _uiState.value = _uiState.value.copy(weatherUiState = WeatherUiState.PermissionRequired)
    }

    fun retryWeather() {
        if (weatherPreferences.isEnabled()) {
            _uiState.value = _uiState.value.copy(weatherUiState = WeatherUiState.Loading)
            refreshWeather()
        }
    }

    fun openWeatherSettings() {
        appSettingsOpener.openAppSettings()
    }

    fun autoGenerateOutfits(occasion: OutfitOccasion) {
        // Only auto-generate if it hasn't been done before
        if (_uiState.value.hasAutoGenerated) return

        selectOccasion(occasion)
        _uiState.value = _uiState.value.copy(hasAutoGenerated = true)
        generateOutfits()
    }

    private fun refreshWeather() {
        viewModelScope.launch {
            when (val location = weatherLocationService.getCurrentLocation()) {
                is WeatherLocationResult.Success -> {
                    runCatching {
                        weatherRepository.getCurrentWeather(
                            latitude = location.latitude,
                            longitude = location.longitude,
                        )
                    }.onSuccess { weather ->
                        _uiState.value = _uiState.value.copy(
                            weatherUiState = WeatherUiState.Loaded(weather),
                        )
                    }.onFailure { throwable ->
                        _uiState.value = _uiState.value.copy(
                            weatherUiState = WeatherUiState.Error(
                                ErrorMapper.toUserMessage(throwable, tag = "OutfitViewModel", context = ErrorContext.Weather),
                            ),
                        )
                    }
                }

                WeatherLocationResult.PermissionRequired -> {
                    markWeatherPermissionRequired()
                }

                is WeatherLocationResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        weatherUiState = WeatherUiState.Error(
                            location.message,
                        ),
                    )
                }
            }
        }
    }
}
