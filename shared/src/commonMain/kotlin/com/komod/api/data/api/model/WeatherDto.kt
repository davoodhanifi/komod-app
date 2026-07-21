package com.komod.api.data.api.model

import kotlinx.serialization.Serializable

@Serializable
data class WeatherDto(
    val temperatureC: Double,
    val feelsLikeC: Double,
    val weatherCode: Int,
    val condition: String,
    val windSpeedKmh: Double,
    val isRaining: Boolean,
    val isSnowing: Boolean,
)
