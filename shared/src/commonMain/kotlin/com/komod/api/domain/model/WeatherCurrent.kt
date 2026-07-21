package com.komod.api.domain.model

data class WeatherCurrent(
    val temperatureC: Double,
    val feelsLikeC: Double,
    val weatherCode: Int,
    val condition: String,
    val windSpeedKmh: Double,
    val isRaining: Boolean,
    val isSnowing: Boolean
)
