package com.komod.api.data.repository

import com.komod.api.domain.model.WeatherCurrent

interface WeatherRepository {
    suspend fun getCurrentWeather(latitude: Double, longitude: Double): WeatherCurrent
}
