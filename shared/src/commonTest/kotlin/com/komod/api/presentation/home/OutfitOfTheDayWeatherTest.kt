package com.komod.api.presentation.home

import kotlin.test.Test
import kotlin.test.assertEquals

class OutfitOfTheDayWeatherTest {
    private fun weather(next6Hours: OutfitOfTheDayWeatherRange? = null) = OutfitOfTheDayWeather(
        temperatureC = 21.0,
        condition = "PartlyCloudy",
        isRaining = false,
        isSnowing = false,
        next6Hours = next6Hours,
    )

    @Test
    fun `headline falls back to the current temperature when next6Hours is missing`() {
        val headline = outfitTemperatureHeadline(weather(next6Hours = null))

        assertEquals("21°", headline)
    }

    @Test
    fun `headline shows the next 6h range when it is known`() {
        val headline = outfitTemperatureHeadline(
            weather(next6Hours = OutfitOfTheDayWeatherRange(minTemperatureC = 18.0, maxTemperatureC = 25.0)),
        )

        assertEquals("18–25°", headline)
    }

    @Test
    fun `headline rounds fractional temperatures`() {
        val headline = outfitTemperatureHeadline(
            weather(next6Hours = OutfitOfTheDayWeatherRange(minTemperatureC = 17.6, maxTemperatureC = 24.5)),
        )

        assertEquals("18–25°", headline)
    }
}
