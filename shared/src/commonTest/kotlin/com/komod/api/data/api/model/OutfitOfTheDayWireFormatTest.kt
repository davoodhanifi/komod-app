package com.komod.api.data.api.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Guards the GET /outfits/today wire shape: a mismatch here would silently break the
 * Outfit of the Day card without any Kotlin compiler error to catch it.
 */
class OutfitOfTheDayWireFormatTest {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Test
    fun `response decodes up to 5 outfits in the fixed direction order`() {
        val body = """
            {
              "outfits": [
                { "name": "Best Match", "reason": "A polished, weather-appropriate look for a mild afternoon.", "wardrobeItemIds": ["a", "b", "c"] },
                { "name": "Casual", "reason": "Relaxed and easy.", "wardrobeItemIds": ["d", "e"] }
              ],
              "weather": {
                "temperatureC": 20.0,
                "feelsLikeC": 19.0,
                "condition": "PartlyCloudy",
                "windSpeedKmh": 8.0,
                "isRaining": false,
                "isSnowing": false,
                "next6HourMinTemperatureC": 14.0,
                "next6HourMaxTemperatureC": 21.0
              }
            }
        """.trimIndent()

        val dto = json.decodeFromString<OutfitOfTheDayResponseDto>(body)

        assertEquals(2, dto.outfits.size)
        assertEquals("Best Match", dto.outfits[0].name)
        assertEquals(listOf("a", "b", "c"), dto.outfits[0].wardrobeItemIds)
        assertEquals("Casual", dto.outfits[1].name)
        assertTrue(dto.outfits[0].items.isEmpty())
        assertEquals(20.0, dto.weather.temperatureC)
        assertEquals(14.0, dto.weather.next6HourMinTemperatureC)
        assertEquals(21.0, dto.weather.next6HourMaxTemperatureC)
    }

    @Test
    fun `response decodes fewer than 5 outfits without padding`() {
        val body = """
            {
              "outfits": [
                { "name": "Best Match", "reason": "Only one direction fit this wardrobe well.", "wardrobeItemIds": ["a"] }
              ],
              "weather": {
                "temperatureC": 20.0,
                "feelsLikeC": 19.0,
                "condition": "PartlyCloudy",
                "windSpeedKmh": 8.0,
                "isRaining": false,
                "isSnowing": false,
                "next6HourMinTemperatureC": 14.0,
                "next6HourMaxTemperatureC": 21.0
              }
            }
        """.trimIndent()

        val dto = json.decodeFromString<OutfitOfTheDayResponseDto>(body)

        assertEquals(1, dto.outfits.size)
    }

    @Test
    fun `weather with the next6Hour range omitted decodes both bounds as null`() {
        val body = """
            {
              "outfits": [],
              "weather": {
                "temperatureC": 20.0,
                "feelsLikeC": 19.0,
                "condition": "PartlyCloudy",
                "windSpeedKmh": 8.0,
                "isRaining": false,
                "isSnowing": false
              }
            }
        """.trimIndent()

        val dto = json.decodeFromString<OutfitOfTheDayResponseDto>(body)

        assertNull(dto.weather.next6HourMinTemperatureC)
        assertNull(dto.weather.next6HourMaxTemperatureC)
    }
}
