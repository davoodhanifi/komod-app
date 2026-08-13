package com.komod.api.domain.model

data class OutfitOfTheDay(
    val outfits: List<Outfit>,
    val weather: OutfitOfTheDayWeatherSnapshot,
)

// The weather snapshot the outfits were generated against — not a live reading, so it's
// shown as-is rather than re-fetched from /weather/current.
data class OutfitOfTheDayWeatherSnapshot(
    val temperatureC: Double,
    val condition: String,
    val isRaining: Boolean,
    val isSnowing: Boolean,
    val next6HourMinTemperatureC: Double?,
    val next6HourMaxTemperatureC: Double?,
)
