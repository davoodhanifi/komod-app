package com.komod.api.data.api

import com.komod.api.data.api.model.WeatherDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import com.komod.api.data.api.model.ResponseData

class WeatherApi(
    private val httpClient: HttpClient,
) {
    suspend fun getCurrentWeather(latitude: Double, longitude: Double): WeatherDto {
        return httpClient.get("weather/current") {
            parameter("latitude", latitude)
            parameter("longitude", longitude)
        }.body<ResponseData<WeatherDto>>().data
    }
}
