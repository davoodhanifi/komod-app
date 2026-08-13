package com.komod.api.data.api.model

import kotlinx.serialization.Serializable

@Serializable
data class OutfitOfTheDayResponseDto(
    val outfits: List<OutfitGeneratedDto>,
    val weather: OutfitOfTheDayWeatherDto,
)

@Serializable
data class OutfitOfTheDayWeatherDto(
    val temperatureC: Double,
    val feelsLikeC: Double,
    val condition: String,
    val windSpeedKmh: Double,
    val isRaining: Boolean,
    val isSnowing: Boolean,
    val next6HourMinTemperatureC: Double? = null,
    val next6HourMaxTemperatureC: Double? = null,
)
