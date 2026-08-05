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

    fun saveOutfit(outfit: Outfit) {
        val state = _uiState.value
        if (outfit.id in state.savingOutfitIds || outfit.id in state.savedOutfitIds) return

        _uiState.update { it.copy(savingOutfitIds = it.savingOutfitIds + outfit.id) }
        viewModelScope.launch {
            runCatching {
                outfitRepository.saveOutfit(outfit)
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        savingOutfitIds = it.savingOutfitIds - outfit.id,
                        savedOutfitIds = it.savedOutfitIds + outfit.id,
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
