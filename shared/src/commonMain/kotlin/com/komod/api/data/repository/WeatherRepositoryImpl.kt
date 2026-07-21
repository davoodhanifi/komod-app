package com.komod.api.data.repository

import com.komod.api.data.api.WeatherApi
import com.komod.api.domain.model.WeatherCurrent

class WeatherRepositoryImpl(
    private val weatherApi: WeatherApi,
) : WeatherRepository {
    override suspend fun getCurrentWeather(latitude: Double, longitude: Double): WeatherCurrent {
        return weatherApi.getCurrentWeather(latitude, longitude).toDomain()
    }
}

private fun com.komod.api.data.api.model.WeatherDto.toDomain(): WeatherCurrent {
    return WeatherCurrent(
        temperatureC = temperatureC,
        feelsLikeC = feelsLikeC,
        weatherCode = weatherCode,
        condition = condition,
        windSpeedKmh = windSpeedKmh,
        isRaining = isRaining,
        isSnowing = isSnowing,
    )
}
