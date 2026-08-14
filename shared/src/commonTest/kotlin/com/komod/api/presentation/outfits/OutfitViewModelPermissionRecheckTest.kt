package com.komod.api.presentation.outfits

import com.komod.api.domain.model.WeatherCurrent
import com.komod.api.domain.model.WeatherLocation
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OutfitViewModelPermissionRecheckTest {
    @Test
    fun `re-checks when the screen is showing the permission-required banner`() {
        assertTrue(shouldRecheckWeatherPermission(WeatherUiState.PermissionRequired))
    }

    @Test
    fun `does not re-check when weather is simply turned off by the user`() {
        assertFalse(shouldRecheckWeatherPermission(WeatherUiState.WeatherDisabled))
    }

    @Test
    fun `does not re-check while a fetch is already in flight`() {
        assertFalse(shouldRecheckWeatherPermission(WeatherUiState.Loading))
    }

    @Test
    fun `does not re-check when weather already loaded successfully`() {
        val weather = WeatherCurrent(
            location = WeatherLocation(
                latitude = 52.37,
                longitude = 4.89,
                timezone = "Europe/Amsterdam",
                city = "Amsterdam",
                neighborhood = null,
            ),
            temperatureC = 18.0,
            feelsLikeC = 17.0,
            weatherCode = 0,
            condition = "Clear",
            windSpeedKmh = 10.0,
            isRaining = false,
            isSnowing = false,
        )
        assertFalse(shouldRecheckWeatherPermission(WeatherUiState.Loaded(weather)))
    }

    @Test
    fun `does not re-check on an unrelated weather error`() {
        assertFalse(shouldRecheckWeatherPermission(WeatherUiState.Error("network error")))
    }
}
