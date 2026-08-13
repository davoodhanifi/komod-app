package com.komod.api.data.api.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Guards the GET /weather/current wire shape, in particular the new next6Hours field: a
 * mismatch here would silently break weather deserialization without any Kotlin compiler
 * error to catch it.
 */
class WeatherDtoWireFormatTest {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun locationJson() = """
        "location": {
          "latitude": 52.37,
          "longitude": 4.89,
          "timezone": "Europe/Amsterdam",
          "city": "Amsterdam",
          "neighborhood": "Jordaan"
        }
    """.trimIndent()

    @Test
    fun `weather response decodes the current temperature and existing fields`() {
        val body = """
            {
              ${locationJson()},
              "temperatureC": 21.0,
              "feelsLikeC": 19.5,
              "weatherCode": 1,
              "condition": "PartlyCloudy",
              "windSpeedKmh": 12.0,
              "isRaining": false,
              "isSnowing": false
            }
        """.trimIndent()

        val dto = json.decodeFromString<WeatherDto>(body)

        assertEquals(21.0, dto.temperatureC)
        assertEquals("Amsterdam", dto.location.city)
        assertEquals("PartlyCloudy", dto.condition)
        assertNull(dto.next6Hours)
    }

    @Test
    fun `weather response decodes next6Hours min and max temperature`() {
        val body = """
            {
              ${locationJson()},
              "temperatureC": 21.0,
              "feelsLikeC": 19.5,
              "weatherCode": 1,
              "condition": "PartlyCloudy",
              "windSpeedKmh": 12.0,
              "isRaining": false,
              "isSnowing": false,
              "next6Hours": {
                "minTemperatureC": 18.0,
                "maxTemperatureC": 25.0
              }
            }
        """.trimIndent()

        val dto = json.decodeFromString<WeatherDto>(body)

        assertEquals(18.0, dto.next6Hours?.minTemperatureC)
        assertEquals(25.0, dto.next6Hours?.maxTemperatureC)
    }

    @Test
    fun `weather response with an explicit null next6Hours decodes to null`() {
        val body = """
            {
              ${locationJson()},
              "temperatureC": 21.0,
              "feelsLikeC": 19.5,
              "weatherCode": 1,
              "condition": "PartlyCloudy",
              "windSpeedKmh": 12.0,
              "isRaining": false,
              "isSnowing": false,
              "next6Hours": null
            }
        """.trimIndent()

        val dto = json.decodeFromString<WeatherDto>(body)

        assertNull(dto.next6Hours)
    }

    @Test
    fun `weather response missing next6Hours entirely still decodes via the default`() {
        val body = """
            {
              ${locationJson()},
              "temperatureC": 21.0,
              "feelsLikeC": 19.5,
              "weatherCode": 1,
              "condition": "PartlyCloudy",
              "windSpeedKmh": 12.0,
              "isRaining": false,
              "isSnowing": false
            }
        """.trimIndent()

        val dto = json.decodeFromString<WeatherDto>(body)

        assertNull(dto.next6Hours)
        assertEquals(21.0, dto.temperatureC)
    }
}
