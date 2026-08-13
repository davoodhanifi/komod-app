package com.komod.api.data.repository

import com.komod.api.data.api.WeatherApi
import com.komod.api.domain.model.WeatherCurrent
import com.komod.api.domain.model.WeatherLocation
import com.komod.api.domain.model.WeatherNext6Hours

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
        isSnowing = isSnowing,
        next6Hours = next6Hours?.let { WeatherNext6Hours(it.minTemperatureC, it.maxTemperatureC) }
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
