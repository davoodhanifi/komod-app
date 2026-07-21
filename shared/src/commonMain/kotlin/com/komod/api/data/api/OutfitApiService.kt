package com.komod.api.data.api

import com.komod.api.data.api.model.OutfitGenerateRequest
import com.komod.api.data.api.model.OutfitGenerateResponse
import com.komod.api.data.api.model.ResponseData
import com.komod.api.data.api.model.WeatherContextDto
import com.komod.api.domain.model.WeatherCurrent
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class OutfitApiService(
    private val httpClient: HttpClient,
) {
    suspend fun generateOutfits(
        occasion: String,
        style: String,
        weather: WeatherCurrent? = null,
    ): OutfitGenerateResponse {
        return httpClient.post("outfits/generate") {
            contentType(ContentType.Application.Json)
            setBody(
                OutfitGenerateRequest(
                    occasion = occasion,
                    style = style,
                    weather = weather?.toWeatherContext(),
                ),
            )
        }.body<ResponseData<OutfitGenerateResponse>>().data
    }
}

private fun WeatherCurrent.toWeatherContext(): WeatherContextDto {
    return WeatherContextDto(
        temperatureC = temperatureC,
        feelsLikeC = feelsLikeC,
        condition = condition,
        windSpeedKmh = windSpeedKmh,
        isRaining = isRaining,
        isSnowing = isSnowing,
    )
}
