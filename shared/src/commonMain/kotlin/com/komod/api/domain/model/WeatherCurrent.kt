package com.komod.api.domain.model

data class WeatherCurrent(
    val location: WeatherLocation,
    val temperatureC: Double,
    val feelsLikeC: Double,
    val weatherCode: Int,
    val condition: String,
    val windSpeedKmh: Double,
    val isRaining: Boolean,
    val isSnowing: Boolean,
    val next6Hours: WeatherNext6Hours? = null
)

data class WeatherNext6Hours(
    val minTemperatureC: Double,
    val maxTemperatureC: Double,
)

data class WeatherLocation(
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    val city: String?,
    val neighborhood: String?
)
