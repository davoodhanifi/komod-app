package com.komod.api.data.repository

import com.komod.api.data.api.WeatherApi
import com.komod.api.domain.model.WeatherCurrent
import com.komod.api.domain.model.WeatherLocation

class WeatherRepositoryImpl(
    private val weatherApi: WeatherApi,
) : WeatherRepository {
    override suspend fun getCurrentWeather(latitude: Double, longitude: Double): WeatherCurrent {
        return weatherApi.getCurrentWeather(latitude, longitude).toDomain()
    }
}

private fun com.komod.api.data.api.model.WeatherDto.toDomain(): WeatherCurrent {
    return WeatherCurrent(
        location = location.toDomain(),
        temperatureC = temperatureC,
        feelsLikeC = feelsLikeC,
        weatherCode = weatherCode,
        condition = condition,
        windSpeedKmh = windSpeedKmh,
        isRaining = isRaining,
        isSnowing = isSnowing
    )
}

private fun com.komod.api.data.api.model.LocationDto.toDomain(): WeatherLocation {
    return WeatherLocation(
        latitude = latitude,
        longitude = longitude,
        timezone = timezone,
        city = city,
        neighborhood = neighborhood
    )
}
