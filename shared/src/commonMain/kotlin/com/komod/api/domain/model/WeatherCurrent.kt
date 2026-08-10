package com.komod.api.domain.model

data class WeatherCurrent(
    val location: WeatherLocation,
    val temperatureC: Double,
    val feelsLikeC: Double,
    val weatherCode: Int,
    val condition: String,
    val windSpeedKmh: Double,
    val isRaining: Boolean,
    val isSnowing: Boolean
)

data class WeatherLocation(
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    val city: String?,
    val neighborhood: String?
)
