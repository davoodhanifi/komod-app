package com.komod.api.presentation.outfits

import com.komod.api.domain.model.WeatherCurrent

sealed interface WeatherUiState {
    data object Loading : WeatherUiState

    data class Loaded(
        val weather: WeatherCurrent,
    ) : WeatherUiState

    data class Error(
        val message: String,
    ) : WeatherUiState

    data object PermissionRequired : WeatherUiState

    data object WeatherDisabled : WeatherUiState
}
